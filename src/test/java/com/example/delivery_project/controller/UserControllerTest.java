package com.example.delivery_project.controller;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.dto.request.UserJoinRequest;
import com.example.delivery_project.dto.request.UserLoginRequest;
import com.example.delivery_project.dto.response.UserInfoResponse;
import com.example.delivery_project.dto.response.UserLoginResponse;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.security.auth.CustomUserDetails;
import com.example.delivery_project.security.jwt.JwtProperties;
import com.example.delivery_project.service.TokenService;
import com.example.delivery_project.service.UserService;
import com.example.delivery_project.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtProperties jwtProperties;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(
                userService,
                tokenService,
                jwtProperties
        );
    }

    @Test
    void 회원가입_요청을_서비스에_전달한다() {
        UserJoinRequest request =
                new UserJoinRequest("driver", "password", "배송기사");

        controller.join(request);

        verify(userService).join(request);
    }

    @Test
    void 로그인하면_AccessToken과_HttpOnly_RefreshToken을_반환한다() {
        UserLoginRequest request =
                new UserLoginRequest("driver", "password");
        when(userService.login(request)).thenReturn(
                new TokenService.TokenPair(
                        "access-token",
                        "refresh-token"
                )
        );
        when(jwtProperties.getRefreshTokenValidity())
                .thenReturn(Duration.ofDays(7));
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        UserLoginResponse result = controller.login(request, response);

        Cookie cookie = response.getCookie(
                CookieUtil.REFRESH_TOKEN_COOKIE
        );
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge())
                .isEqualTo((int) Duration.ofDays(7).toSeconds());
    }

    @Test
    void 로그아웃하면_DB토큰과_쿠키를_삭제한다() {
        User user = user();
        CustomUserDetails details = new CustomUserDetails(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "refresh-token"
        ));
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        controller.logout(details, request, response);

        verify(tokenService).logout(1L);
        Cookie deleted = response.getCookie(
                CookieUtil.REFRESH_TOKEN_COOKIE
        );
        assertThat(deleted).isNotNull();
        assertThat(deleted.getMaxAge()).isZero();
    }

    @Test
    void 로그인_사용자의_프로필을_반환한다() {
        User user = user();

        UserInfoResponse result = controller.getMyInfo(
                new CustomUserDetails(user)
        );

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.loginId()).isEqualTo("driver");
        assertThat(result.name()).isEqualTo("배송기사");
        assertThat(result.role()).isEqualTo(Role.ROLE_DELIVERY_DRIVER);
    }

    private User user() {
        return User.of(
                1L,
                "driver",
                "password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
    }
}
