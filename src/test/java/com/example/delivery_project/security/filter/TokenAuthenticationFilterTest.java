package com.example.delivery_project.security.filter;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.security.jwt.TokenProvider;
import com.example.delivery_project.security.jwt.TokenStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TokenAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_Bearer_토큰이면_인증정보를_SecurityContext에_저장한다()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = User.of(
                1L,
                "driver",
                null,
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        "access-token"
                );
        when(tokenProvider.validateToken("access-token"))
                .thenReturn(TokenStatus.VALID);
        when(tokenProvider.getTokenDetails("access-token"))
                .thenReturn(user);
        when(tokenProvider.getAuthentication(user, "access-token"))
                .thenReturn(authentication);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isEqualTo(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰이_없어도_다음_필터를_실행한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
        verifyNoInteractions(tokenProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 유효하지_않은_토큰은_인증정보를_만들지_않는다()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenProvider.validateToken("invalid-token"))
                .thenReturn(TokenStatus.INVALID);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
        verify(tokenProvider, never()).getTokenDetails("invalid-token");
        verify(filterChain).doFilter(request, response);
    }
}
