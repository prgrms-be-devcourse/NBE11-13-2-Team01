package com.example.delivery_project.security.jwt;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.security.auth.CustomUserDetails;
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

    private static final String CLAIM_ID = "id";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;

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
    public String generateToken(User user, Duration validity) {

        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + validity.toMillis());

        // token에 넣을 정보 : iss, sub, iat, exp, role, name, id
        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiredAt)
                .subject(user.getLoginId())
                .claim(CLAIM_ID, user.getId())
                .claim(CLAIM_NAME, user.getName())
                .claim(CLAIM_ROLE, user.getRole().name())
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    // token 검증 결과를 TokenStatus로 반환
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

    // token의 claim 추출
    private Claims getClaims(String token) {
        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }

    // 검증 완료된 token을 소유한 User 정보 복구
    public User getTokenDetails(String token) {

        Claims claims = getClaims(token);

        return User.of(
                claims.get(CLAIM_ID, Long.class),
                claims.getSubject(),
                null,
                claims.get(CLAIM_NAME, String.class),
                Role.valueOf(claims.get(CLAIM_ROLE, String.class))
        );
    }

    // 검증 완료된 토큰을 가진 User에 대하여 인증 객체 생성
    public Authentication getAuthentication(User user, String token) {

        CustomUserDetails principal = new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }
}
