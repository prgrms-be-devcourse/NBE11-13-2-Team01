package com.example.delivery_project.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    // [Key] - [Value] - [TTL]
    // [refreshToken:userId] - [R1] - [TTL]
    private static final String KEY_PREFIX = "refreshToken:";
    private final StringRedisTemplate redisTemplate;

    // redis에 refresh token 저장
    // 최초 로그인, 토큰 재발급
    // SET [refreshToken:userId] [token] EX [ttl]
    public void save(Long userId, String token, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, token, ttl);
    }

    // redis에 있는 refresh token 문자열 찾기
    // 토큰 2차 검증
    // GET [refreshToken:userId]
    public Optional<String> findByUserId(Long userId) {
        String token = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return Optional.ofNullable(token);
    }

    // redis에 있는 refresh token 삭제
    // 로그아웃
    // DEL [refreshToken:userId]
    public void deleteByUserId(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
