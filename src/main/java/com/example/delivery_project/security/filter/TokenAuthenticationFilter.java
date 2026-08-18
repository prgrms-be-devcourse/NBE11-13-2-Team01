package com.example.delivery_project.security.filter;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.security.jwt.TokenProvider;
import com.example.delivery_project.security.jwt.TokenStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        // 추출한 access token이 유효하다면
        // Authentication 생성 후 SecurityContextHolder에 저장
        if(token != null) {

            TokenStatus status = tokenProvider.validateToken(token);

            if(status == TokenStatus.VALID) {

                User user = tokenProvider.getTokenDetails(token);
                Authentication authentication = tokenProvider.getAuthentication(user, token);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authentication success. userId: {}, uri: {}", user.getId(), request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }

    // request의 Authorization 헤더에서 access token 추출
    private String resolveToken(HttpServletRequest request) {

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
