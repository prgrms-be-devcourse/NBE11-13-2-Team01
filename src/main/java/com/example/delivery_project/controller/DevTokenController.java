package com.example.delivery_project.controller;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.response.TokenResponse;
import com.example.delivery_project.exception.ExceptionCode;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.security.jwt.JwtProperties;
import com.example.delivery_project.security.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/tokens")
public class DevTokenController {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @PostMapping
    public TokenResponse issueAccessToken(
            @RequestParam String loginId
    ) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(
                        ExceptionCode.INVALID_INPUT,
                        "존재하지 않는 loginId입니다: " + loginId
                ));

        String accessToken = tokenProvider.generateToken(
                user,
                jwtProperties.getAccessTokenValidity()
        );

        return new TokenResponse(accessToken);
    }
}
