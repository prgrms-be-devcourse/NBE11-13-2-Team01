package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.token.RefreshToken;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.RefreshTokenRepository;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.exception.AuthException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.security.jwt.JwtProperties;
import com.example.delivery_project.security.jwt.TokenProvider;
import com.example.delivery_project.security.jwt.TokenStatus;
import com.example.delivery_project.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final Duration ACCESS_VALIDITY = Duration.ofHours(2);
    private static final Duration REFRESH_VALIDITY = Duration.ofDays(7);

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.of(
                1L,
                "driver",
                "password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
    }

    @Test
    void 토큰을_발급하고_신규_RefreshToken을_저장한다() {
        givenTokenProperties();
        when(tokenProvider.generateToken(user, ACCESS_VALIDITY))
                .thenReturn("access-token");
        when(tokenProvider.generateToken(user, REFRESH_VALIDITY))
                .thenReturn("refresh-token");
        when(refreshTokenRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        TokenService.TokenPair pair = tokenService.issueToken(user);

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        assertThat(pair.accessToken()).isEqualTo("access-token");
        assertThat(pair.refreshToken()).isEqualTo("refresh-token");
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getToken()).isEqualTo("refresh-token");
    }

    @Test
    void 기존_RefreshToken이_있으면_새_토큰으로_교체한다() {
        givenTokenProperties();
        RefreshToken stored = RefreshToken.of(user, "old-token");
        when(tokenProvider.generateToken(user, ACCESS_VALIDITY))
                .thenReturn("new-access-token");
        when(tokenProvider.generateToken(user, REFRESH_VALIDITY))
                .thenReturn("new-refresh-token");
        when(refreshTokenRepository.findByUserId(1L))
                .thenReturn(Optional.of(stored));

        tokenService.issueToken(user);

        assertThat(stored.getToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void 유효한_RefreshToken으로_rotation을_수행한다() {
        givenTokenProperties();
        RefreshToken stored = RefreshToken.of(user, "old-refresh-token");
        Cookie cookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "old-refresh-token"
        );
        when(tokenProvider.validateToken("old-refresh-token"))
                .thenReturn(TokenStatus.VALID);
        when(refreshTokenRepository.findByToken("old-refresh-token"))
                .thenReturn(Optional.of(stored));
        when(tokenProvider.generateToken(user, ACCESS_VALIDITY))
                .thenReturn("new-access-token");
        when(tokenProvider.generateToken(user, REFRESH_VALIDITY))
                .thenReturn("new-refresh-token");
        when(refreshTokenRepository.findByUserId(1L))
                .thenReturn(Optional.of(stored));

        TokenService.TokenPair pair =
                tokenService.refreshToken(new Cookie[]{cookie});

        assertThat(pair.accessToken()).isEqualTo("new-access-token");
        assertThat(pair.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(stored.getToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void RefreshToken_쿠키가_없으면_재발급을_거부한다() {
        assertAuthException(
                () -> tokenService.refreshToken(null),
                AuthException.REFRESH_TOKEN_NOT_FOUND
        );
    }

    @Test
    void 만료되거나_위조된_RefreshToken을_거부한다() {
        Cookie expiredCookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "expired-token"
        );
        when(tokenProvider.validateToken("expired-token"))
                .thenReturn(TokenStatus.EXPIRED);

        assertAuthException(
                () -> tokenService.refreshToken(
                        new Cookie[]{expiredCookie}
                ),
                AuthException.EXPIRED_REFRESH_TOKEN
        );

        Cookie invalidCookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "invalid-token"
        );
        when(tokenProvider.validateToken("invalid-token"))
                .thenReturn(TokenStatus.INVALID);

        assertAuthException(
                () -> tokenService.refreshToken(
                        new Cookie[]{invalidCookie}
                ),
                AuthException.INVALID_REFRESH_TOKEN
        );
    }

    @Test
    void DB에_없는_RefreshToken을_거부한다() {
        Cookie cookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "unknown-token"
        );
        when(tokenProvider.validateToken("unknown-token"))
                .thenReturn(TokenStatus.VALID);
        when(refreshTokenRepository.findByToken("unknown-token"))
                .thenReturn(Optional.empty());

        assertAuthException(
                () -> tokenService.refreshToken(new Cookie[]{cookie}),
                AuthException.INVALID_REFRESH_TOKEN
        );
    }

    @Test
    void 로그아웃하면_사용자의_RefreshToken을_삭제한다() {
        tokenService.logout(1L);

        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    private void givenTokenProperties() {
        when(jwtProperties.getAccessTokenValidity())
                .thenReturn(ACCESS_VALIDITY);
        when(jwtProperties.getRefreshTokenValidity())
                .thenReturn(REFRESH_VALIDITY);
    }

    private void assertAuthException(
            Runnable action,
            AuthException expected
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expected)
                );
    }
}
