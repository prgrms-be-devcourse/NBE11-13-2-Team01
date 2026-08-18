package com.example.delivery_project.controller;

import com.example.delivery_project.dto.request.JoinRequest;
import com.example.delivery_project.dto.request.LoginRequest;
import com.example.delivery_project.dto.response.LoginResponse;
import com.example.delivery_project.security.auth.CustomUserDetails;
import com.example.delivery_project.security.jwt.JwtProperties;
import com.example.delivery_project.service.TokenService;
import com.example.delivery_project.service.UserService;
import com.example.delivery_project.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    @PostMapping("/join")
    public ResponseEntity<Void> join(@Valid @RequestBody JoinRequest request) {

        userService.join(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {

        // 로그인하여 발급한 토큰쌍
        TokenService.TokenPair tokenPair = userService.login(request);

        // refresh token은 Http Only Cookie에
        CookieUtil.addCookie(
                response,
                CookieUtil.REFRESH_TOKEN_COOKIE,
                tokenPair.refreshToken(),
                (int) jwtProperties.getRefreshTokenValidity().toSeconds()
        );

        // access token은 response body에
        return ResponseEntity.ok(new LoginResponse(tokenPair.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long userId = userDetails.getUser().getId();

        // DB에 저장되어있던 refresh token 삭제
        tokenService.logout(userId);

        // refresh token 쿠키 삭제
        CookieUtil.deleteCookie(request, response, CookieUtil.REFRESH_TOKEN_COOKIE);

        return ResponseEntity.ok().build();
    }
}
