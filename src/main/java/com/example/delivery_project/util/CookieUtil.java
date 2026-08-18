package com.example.delivery_project.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private CookieUtil(){}

    public static void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAge
    ) {
        Cookie cookie = new Cookie(name,value);
        cookie.setHttpOnly(true);
        // 현재 프로젝트에서 배포하지 않기 때문에 임시 false
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String name
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return;
        }

        for(Cookie cookie : cookies) {
            if(name.equals(cookie.getName())) {
                cookie.setMaxAge(0);
                cookie.setPath("/");
                cookie.setValue("");
                response.addCookie(cookie);
            }
        }
    }
}
