package com.example.delivery_project.security.jwt;

import com.example.delivery_project.security.auth.CustomUserDetails;
import com.example.delivery_project.security.auth.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenProvider {

    private final JwtProperties jwtProperties;
    private final CustomUserDetailsService customUserDetailsService;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    private void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.getSecretKey()));
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build();
    }

    // JWT 문자열 생성
    public String generateToken(String loginId, Duration validity) {

        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + validity.toMillis());

        // token에 넣을 정보 : iss, sub, iat, exp (role, name, id 제외)
        // User의 정보는 loginId(sub)만 넣는다. -> 인자에 User 객체 통으로 넣지 않아도 됨
        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(jwtProperties.getIssuer())
                .subject(loginId)
                .issuedAt(now)
                .expiration(expiredAt)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    // access token 생성
    public String generateAccessToken(String loginId) {
        return generateToken(loginId, jwtProperties.getAccessTokenValidity());
    }

    // refresh token 생성
    public String generateRefreshToken(String loginId) {
        return generateToken(loginId, jwtProperties.getRefreshTokenValidity());
    }

    // 토큰 검증 결과를 TokenStatus로 반환
    public TokenStatus validateToken(String token) {

        try {
            jwtParser.parseSignedClaims(token);
            log.debug("Token is valid");
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            log.warn("Token is expired");
            return TokenStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token is invalid");
            return TokenStatus.INVALID;
        }
    }

    // token을 소유한 User의 loginId 찾기
    public String getLoginId(String token) {

        Claims claims = jwtParser.parseSignedClaims(token).getPayload();

        return claims.getSubject();
    }


    // 검증 완료된 토큰을 가진 사용자에 대하여 인증 객체 생성
    public Authentication getAuthentication(String token) {

        String loginId = getLoginId(token);

        CustomUserDetails principal = customUserDetailsService.loadUserByUsername(loginId);

        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }
}
