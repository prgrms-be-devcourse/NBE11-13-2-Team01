package com.example.delivery_project.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CookieUtilTest {

    @Test
    void HttpOnly_RefreshToken_쿠키를_추가한다() {
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        CookieUtil.addCookie(
                response,
                CookieUtil.REFRESH_TOKEN_COOKIE,
                "refresh-token",
                3600
        );

        Cookie cookie = response.getCookie(
                CookieUtil.REFRESH_TOKEN_COOKIE
        );
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(3600);
    }

    @Test
    void 로그아웃하면_동일한_이름의_쿠키를_만료시킨다() {
        MockHttpServletRequest request =
                new MockHttpServletRequest();
        request.setCookies(
                new Cookie(CookieUtil.REFRESH_TOKEN_COOKIE, "token"),
                new Cookie("other", "value")
        );
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        CookieUtil.deleteCookie(
                request,
                response,
                CookieUtil.REFRESH_TOKEN_COOKIE
        );

        Cookie deleted = response.getCookie(
                CookieUtil.REFRESH_TOKEN_COOKIE
        );
        assertThat(deleted).isNotNull();
        assertThat(deleted.getValue()).isEmpty();
        assertThat(deleted.getMaxAge()).isZero();
        assertThat(deleted.getPath()).isEqualTo("/");
    }

    @Test
    void 요청에_쿠키가_없어도_삭제는_실패하지_않는다() {
        assertThatCode(() -> CookieUtil.deleteCookie(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                CookieUtil.REFRESH_TOKEN_COOKIE
        )).doesNotThrowAnyException();
    }
}
