package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.token.RefreshToken;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.RefreshTokenRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {
    }

    // access token, refresh token 발급
    // 발급한 refresh token을 DB에도 저장
    @Transactional
    public TokenPair issueToken(User user) {
        String accessToken = tokenProvider.generateToken(user, jwtProperties.getAccessTokenValidity());
        String refreshToken = tokenProvider.generateToken(user, jwtProperties.getRefreshTokenValidity());

       saveRefreshToken(user, refreshToken);
        log.debug("Token issued. userId: {}", user.getId());

        return new TokenPair(accessToken, refreshToken);
    }

    // refresh token을 DB에 새로 저장 혹은 기존 값 업데이트
    private void saveRefreshToken(User user, String token) {

        // 해당 User의 refresh token 찾기
        RefreshToken refreshToken = refreshTokenRepository
                .findByUserId(user.getId())
                .orElse(null);

        // 새로 가입한 회원이라면 refresh token을 새로 저장
        if(refreshToken == null) {
            refreshTokenRepository.save(RefreshToken.of(user, token));
            return;
        }

        // 기존 회원이라면 재발급한 refresh token으로 업데이트
        refreshToken.updateToken(token);
    }

    // access token, refresh token 재발급 (refresh token rotation)
    @Transactional
    public TokenPair refreshToken(Cookie[] cookies) {

        // cookie에서 refresh token 추출
        String refreshToken = getRefreshToken(cookies);

        if(refreshToken == null) {
            throw new BusinessException(AuthException.REFRESH_TOKEN_NOT_FOUND);
        }

        // 1. 자체 유효성 검사
        TokenStatus status = tokenProvider.validateToken(refreshToken);

        if(status == TokenStatus.EXPIRED) {
            throw new BusinessException(AuthException.EXPIRED_REFRESH_TOKEN);
        } else if(status == TokenStatus.INVALID) {
            throw new BusinessException(AuthException.INVALID_REFRESH_TOKEN);
        }

        // 2. DB에 저장되어 있는 refresh token인지 검사
        RefreshToken storedRefreshToken = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(AuthException.INVALID_REFRESH_TOKEN));

        // 검증된 refresh token을 통해 access token, refresh token 재발급 (refresh token rotation)
        // refresh token에서 User를 직접 추출(getTokenDetails())하는 것이 아닌
        // refresh token과 연결된 현재 DB의 User 추출
        User user = storedRefreshToken.getUser();

        TokenPair tokenPair = issueToken(user);

        log.debug("Token refreshed. userId: {}", user.getId());

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

    // 로그아웃 시 DB에 저장된 refresh token(row 전체) 삭제
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("[AUTH] 로그아웃 처리 완료 userId: {}", userId);
    }
}
