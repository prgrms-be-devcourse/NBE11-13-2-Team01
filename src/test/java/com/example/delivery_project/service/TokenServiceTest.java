package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.RefreshTokenRedisRepository;
import com.example.delivery_project.domain.repository.UserRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final Duration ACCESS_VALIDITY = Duration.ofHours(2);
    private static final Duration REFRESH_VALIDITY = Duration.ofDays(7);

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Mock
    private UserRepository userRepository;

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
    void 토큰을_발급하고_RefreshToken을_Redis에_저장한다() {
        givenTokenProperties();

        when(tokenProvider.generateToken(user, ACCESS_VALIDITY))
                .thenReturn("access-token");
        when(tokenProvider.generateToken(user, REFRESH_VALIDITY))
                .thenReturn("refresh-token");

        TokenService.TokenPair pair =
                tokenService.issueToken(user);

        assertThat(pair.accessToken())
                .isEqualTo("access-token");
        assertThat(pair.refreshToken())
                .isEqualTo("refresh-token");

        verify(refreshTokenRedisRepository)
                .save(
                        1L,
                        "refresh-token",
                        REFRESH_VALIDITY
                );
    }

    @Test
    void 유효한_RefreshToken으로_rotation을_수행한다() {
        givenTokenProperties();

        Cookie cookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "old-refresh-token"
        );

        // Refresh Token 자체 검증
        when(tokenProvider.validateToken("old-refresh-token"))
                .thenReturn(TokenStatus.VALID);

        // JWT에서 userId 추출
        when(tokenProvider.getTokenDetails("old-refresh-token"))
                .thenReturn(user);

        // Redis에 현재 사용 가능한 Refresh Token 존재
        when(refreshTokenRedisRepository.findByUserId(1L))
                .thenReturn(Optional.of("old-refresh-token"));

        // 실제 DB의 최신 User 조회
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // 새 토큰 발급
        when(tokenProvider.generateToken(user, ACCESS_VALIDITY))
                .thenReturn("new-access-token");
        when(tokenProvider.generateToken(user, REFRESH_VALIDITY))
                .thenReturn("new-refresh-token");

        TokenService.TokenPair pair =
                tokenService.refreshToken(
                        new Cookie[]{cookie}
                );

        assertThat(pair.accessToken())
                .isEqualTo("new-access-token");
        assertThat(pair.refreshToken())
                .isEqualTo("new-refresh-token");

        // 기존 key에 R2 저장 -> R1에서 R2로 rotation
        verify(refreshTokenRedisRepository)
                .save(
                        1L,
                        "new-refresh-token",
                        REFRESH_VALIDITY
                );
    }

    @Test
    void RefreshToken_쿠키가_없으면_재발급을_거부한다() {
        assertAuthException(
                () -> tokenService.refreshToken(null),
                AuthException.REFRESH_TOKEN_NOT_FOUND
        );
    }

    @Test
    void 만료된_RefreshToken을_거부한다() {
        Cookie cookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "expired-token"
        );

        when(tokenProvider.validateToken("expired-token"))
                .thenReturn(TokenStatus.EXPIRED);

        assertAuthException(
                () -> tokenService.refreshToken(
                        new Cookie[]{cookie}
                ),
                AuthException.EXPIRED_REFRESH_TOKEN
        );
    }

    @Test
    void 위조된_RefreshToken을_거부한다() {
        Cookie cookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "invalid-token"
        );

        when(tokenProvider.validateToken("invalid-token"))
                .thenReturn(TokenStatus.INVALID);

        assertAuthException(
                () -> tokenService.refreshToken(
                        new Cookie[]{cookie}
                ),
                AuthException.INVALID_REFRESH_TOKEN
        );
    }

    @Test
    void Redis에_RefreshToken이_없으면_재발급을_거부한다() {
        Cookie cookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "refresh-token"
        );

        when(tokenProvider.validateToken("refresh-token"))
                .thenReturn(TokenStatus.VALID);

        when(tokenProvider.getTokenDetails("refresh-token"))
                .thenReturn(user);

        when(refreshTokenRedisRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertAuthException(
                () -> tokenService.refreshToken(
                        new Cookie[]{cookie}
                ),
                AuthException.INVALID_REFRESH_TOKEN
        );
    }

    @Test
    void Redis에_저장된_RefreshToken과_다르면_재발급을_거부한다() {
        Cookie cookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "old-refresh-token"
        );

        when(tokenProvider.validateToken("old-refresh-token"))
                .thenReturn(TokenStatus.VALID);

        when(tokenProvider.getTokenDetails("old-refresh-token"))
                .thenReturn(user);

        // Redis에는 이미 rotation 된 R2가 들어있는 상황
        when(refreshTokenRedisRepository.findByUserId(1L))
                .thenReturn(Optional.of("new-refresh-token"));

        assertAuthException(
                () -> tokenService.refreshToken(
                        new Cookie[]{cookie}
                ),
                AuthException.INVALID_REFRESH_TOKEN
        );
    }

    @Test
    void RefreshToken의_User가_DB에_없으면_재발급을_거부한다() {
        Cookie cookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "refresh-token"
        );

        when(tokenProvider.validateToken("refresh-token"))
                .thenReturn(TokenStatus.VALID);

        when(tokenProvider.getTokenDetails("refresh-token"))
                .thenReturn(user);

        when(refreshTokenRedisRepository.findByUserId(1L))
                .thenReturn(Optional.of("refresh-token"));

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertAuthException(
                () -> tokenService.refreshToken(
                        new Cookie[]{cookie}
                ),
                AuthException.INVALID_REFRESH_TOKEN
        );
    }

    @Test
    void 로그아웃하면_Redis의_RefreshToken을_삭제한다() {
        tokenService.logout(1L);

        verify(refreshTokenRedisRepository)
                .deleteByUserId(1L);
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
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(expected)
                );
    }
}