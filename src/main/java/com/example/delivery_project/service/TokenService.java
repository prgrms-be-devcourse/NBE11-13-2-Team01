package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.RefreshTokenRedisRepository;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.exception.AuthException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.security.jwt.JwtProperties;
import com.example.delivery_project.security.jwt.TokenProvider;
import com.example.delivery_project.security.jwt.TokenStatus;
import com.example.delivery_project.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    // DB -> Redis 버전 repository로 교체
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final UserRepository userRepository;

    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {
    }

    // access token, refresh token 발급
    // 발급한 refresh token을 Redis에도 저장
    public TokenPair issueToken(User user) {
        String accessToken = tokenProvider.generateToken(user, jwtProperties.getAccessTokenValidity());
        String refreshToken = tokenProvider.generateToken(user, jwtProperties.getRefreshTokenValidity());

        saveRefreshToken(user, refreshToken);
        log.debug("Token issued. userId: {}", user.getId());

        return new TokenPair(accessToken, refreshToken);
    }

    // refresh token을 Redis에 저장
    private void saveRefreshToken(User user, String token) {
        refreshTokenRedisRepository.save(user.getId(), token, jwtProperties.getRefreshTokenValidity());
    }

     // access token, refresh token 재발급 (refresh token rotation)
    public TokenPair refreshToken(Cookie[] cookies) {

        // cookie에서 refresh token 추출
        String refreshToken = getRefreshToken(cookies);

        if(refreshToken == null) {
            throw new BusinessException(AuthException.REFRESH_TOKEN_NOT_FOUND);
        }

        // 1. JWT 자체 유효성 검사
        TokenStatus status = tokenProvider.validateToken(refreshToken);

        if(status == TokenStatus.EXPIRED) {
            throw new BusinessException(AuthException.EXPIRED_REFRESH_TOKEN);
        } else if(status == TokenStatus.INVALID) {
            throw new BusinessException(AuthException.INVALID_REFRESH_TOKEN);
        }

        // 2. 검증된 refresh token에서 userId 추출
        User tokenUser = tokenProvider.getTokenDetails(refreshToken);
        Long userId = tokenUser.getId();

        // 3. userId로 Redis에 저장되어 있는 refresh token인지 검사
        String storedRefreshToken = refreshTokenRedisRepository
                .findByUserId(userId)
                .orElseThrow(() -> new BusinessException(AuthException.INVALID_REFRESH_TOKEN));

        if(!storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(AuthException.INVALID_REFRESH_TOKEN);
        }

        // 4. userId로 DB에서 현재 User 조회
        User dbUser = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(AuthException.INVALID_REFRESH_TOKEN));

        // 5. 조회한 User 정보로 access token, refresh token 재발급 (refresh token rotation)
        TokenPair tokenPair = issueToken(dbUser);

        log.debug("Token refreshed. userId: {}", dbUser.getId());

        return tokenPair;
    }

    // refresh token이 담긴 cookie 찾기
    private String getRefreshToken(Cookie[] cookies) {

        if(cookies == null) return null;

        for(Cookie cookie : cookies) {
            if(cookie.getName().equals(CookieUtil.REFRESH_TOKEN_COOKIE)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // 로그아웃 시 Redis에 저장된 refresh token 삭제
    public void logout(Long userId) {
        refreshTokenRedisRepository.deleteByUserId(userId);
        log.info("[AUTH] 로그아웃 처리 완료 userId: {}", userId);
    }
}
