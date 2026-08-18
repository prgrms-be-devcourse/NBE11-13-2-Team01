package com.example.delivery_project.controller;

import com.example.delivery_project.dto.response.TokenResponse;
import com.example.delivery_project.security.jwt.JwtProperties;
import com.example.delivery_project.service.TokenService;
import com.example.delivery_project.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private TokenController controller;

    @Test
    void RefreshToken을_rotation하고_새_토큰쌍을_반환한다() {
        Cookie oldCookie = new Cookie(
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "old-refresh-token"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(oldCookie);
        MockHttpServletResponse response =
                new MockHttpServletResponse();
        when(tokenService.refreshToken(request.getCookies()))
                .thenReturn(new TokenService.TokenPair(
                        "new-access-token",
                        "new-refresh-token"
                ));
        when(jwtProperties.getRefreshTokenValidity())
                .thenReturn(Duration.ofDays(7));

        TokenResponse result = controller.refreshToken(request, response);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(response.getCookie(
                CookieUtil.REFRESH_TOKEN_COOKIE
        ).getValue()).isEqualTo("new-refresh-token");
        verify(tokenService).refreshToken(request.getCookies());
    }
}
