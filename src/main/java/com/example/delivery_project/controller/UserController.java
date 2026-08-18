package com.example.delivery_project.controller;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.dto.request.UserJoinRequest;
import com.example.delivery_project.dto.request.UserLoginRequest;
import com.example.delivery_project.dto.response.UserLoginResponse;
import com.example.delivery_project.dto.response.UserInfoResponse;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    @PostMapping("/join")
    public void join(@Valid @RequestBody UserJoinRequest request) {

        userService.join(request);

    }

    @PostMapping("/login")
    public UserLoginResponse login(@Valid @RequestBody UserLoginRequest request, HttpServletResponse response) {

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
        return new UserLoginResponse(tokenPair.accessToken());
    }

    @PostMapping("/logout")
    public void logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long userId = userDetails.getUser().getId();

        // DB에 저장되어있던 refresh token 삭제
        tokenService.logout(userId);

        // refresh token 쿠키 삭제
        CookieUtil.deleteCookie(request, response, CookieUtil.REFRESH_TOKEN_COOKIE);
    }

    @GetMapping("/info")
    public UserInfoResponse getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        return new UserInfoResponse(
                        user.getId(),
                        user.getLoginId(),
                        user.getName(),
                        user.getRole()
        );
    }
}
