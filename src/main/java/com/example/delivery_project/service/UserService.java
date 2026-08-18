package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.request.JoinRequest;
import com.example.delivery_project.dto.request.LoginRequest;
import com.example.delivery_project.exception.AuthException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @Transactional
    public void join(JoinRequest request) {

        // 로그인 아이디 중복 검사
        if(userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(AuthException.DUPLICATE_LOGIN_ID);
        }

        User user = request.toUser(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        log.info("[AUTH] 회원가입 성공 userId: {}, loginId: {} role: {}", user.getId(), user.getLoginId(), user.getRole());
    }

    @Transactional
    public TokenService.TokenPair login(LoginRequest request) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
            );

            User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

            TokenService.TokenPair tokenPair = tokenService.issueToken(user);

            log.info("[AUTH] 로그인 성공 userId: {}, loginId: {}", user.getId(), user.getLoginId());

            return tokenPair;

        } catch (AuthenticationException e) {
            throw new BusinessException(AuthException.INVALID_LOGIN);
        }
    }
}
