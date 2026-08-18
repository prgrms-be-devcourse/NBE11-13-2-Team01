package com.example.delivery_project.security.jwt;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.security.auth.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderTest {

    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("delivery-insight-test");
        properties.setSecretKey(
                Base64.getEncoder().encodeToString(new byte[64])
        );

        tokenProvider = new TokenProvider(properties);
        ReflectionTestUtils.invokeMethod(tokenProvider, "init");
    }

    @Test
    void JWT를_생성하고_사용자_정보와_인증객체를_복구한다() {
        User user = user();

        String token = tokenProvider.generateToken(
                user,
                Duration.ofMinutes(10)
        );

        User restored = tokenProvider.getTokenDetails(token);
        Authentication authentication =
                tokenProvider.getAuthentication(restored, token);

        assertThat(tokenProvider.validateToken(token))
                .isEqualTo(TokenStatus.VALID);
        assertThat(restored.getId()).isEqualTo(user.getId());
        assertThat(restored.getLoginId()).isEqualTo(user.getLoginId());
        assertThat(restored.getName()).isEqualTo(user.getName());
        assertThat(restored.getRole()).isEqualTo(user.getRole());
        assertThat(authentication.getPrincipal())
                .isInstanceOf(CustomUserDetails.class);
        assertThat(authentication.getCredentials()).isEqualTo(token);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly(Role.ROLE_DELIVERY_DRIVER.name());
    }

    @Test
    void 만료된_JWT를_구분한다() {
        String token = tokenProvider.generateToken(
                user(),
                Duration.ofSeconds(-1)
        );

        assertThat(tokenProvider.validateToken(token))
                .isEqualTo(TokenStatus.EXPIRED);
    }

    @Test
    void 위조되거나_형식이_잘못된_JWT를_구분한다() {
        assertThat(tokenProvider.validateToken("not-a-jwt"))
                .isEqualTo(TokenStatus.INVALID);
    }

    private User user() {
        return User.of(
                10L,
                "driver",
                "encoded-password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
    }
}
