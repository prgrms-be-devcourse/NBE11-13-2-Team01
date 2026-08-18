package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.request.UserJoinRequest;
import com.example.delivery_project.dto.request.UserLoginRequest;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.exception.AuthException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.security.auth.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void 회원가입하면_비밀번호를_암호화하고_DRIVER로_저장한다() {
        UserJoinRequest request =
                new UserJoinRequest("driver", "plain", "배송기사");
        when(userRepository.existsByLoginId("driver"))
                .thenReturn(false);
        when(passwordEncoder.encode("plain"))
                .thenReturn("encoded");

        userService.join(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.getLoginId()).isEqualTo("driver");
        assertThat(savedUser.getPassword()).isEqualTo("encoded");
        assertThat(savedUser.getName()).isEqualTo("배송기사");
        assertThat(savedUser.getRole())
                .isEqualTo(Role.ROLE_DELIVERY_DRIVER);
    }

    @Test
    void 중복_아이디는_회원가입할_수_없다() {
        UserJoinRequest request =
                new UserJoinRequest("driver", "plain", "배송기사");
        when(userRepository.existsByLoginId("driver"))
                .thenReturn(true);

        assertAuthException(
                () -> userService.join(request),
                AuthException.DUPLICATE_LOGIN_ID
        );

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void 로그인에_성공하면_인증된_사용자의_토큰을_발급한다() {
        User user = User.of(
                1L,
                "driver",
                "encoded",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
        Authentication authentication =
                org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(new CustomUserDetails(user));
        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        TokenService.TokenPair expected =
                new TokenService.TokenPair("access", "refresh");
        when(tokenService.issueToken(user)).thenReturn(expected);

        TokenService.TokenPair result = userService.login(
                new UserLoginRequest("driver", "plain")
        );

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(
                        UsernamePasswordAuthenticationToken.class
                );
        verify(authenticationManager).authenticate(captor.capture());

        assertThat(result).isEqualTo(expected);
        assertThat(captor.getValue().getPrincipal()).isEqualTo("driver");
        assertThat(captor.getValue().getCredentials()).isEqualTo("plain");
        verify(tokenService).issueToken(user);
    }

    @Test
    void 인증에_실패하면_INVALID_LOGIN을_반환한다() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertAuthException(
                () -> userService.login(
                        new UserLoginRequest("driver", "wrong")
                ),
                AuthException.INVALID_LOGIN
        );

        verify(tokenService, never()).issueToken(any());
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
