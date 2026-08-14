package com.example.delivery_project.controller;

import com.example.delivery_project.dto.response.TokenResponse;
import com.example.delivery_project.security.jwt.JwtProperties;
import com.example.delivery_project.service.TokenService;
import com.example.delivery_project.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tokens")
public class TokenController {

    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        // 재발급한 토큰쌍
        TokenService.TokenPair tokenPair = tokenService.refreshToken(request.getCookies());

        // refresh token은 Http Only Cookie에
        CookieUtil.addCookie(
                response,
                CookieUtil.REFRESH_TOKEN_COOKIE,
                tokenPair.refreshToken(),
                (int) jwtProperties.getRefreshTokenValidity().toSeconds()
        );

        // access token은 response body에
        return ResponseEntity.ok(new TokenResponse(tokenPair.accessToken()));
    }
}
