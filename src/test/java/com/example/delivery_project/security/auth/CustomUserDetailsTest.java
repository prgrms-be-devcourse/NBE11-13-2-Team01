package com.example.delivery_project.security.auth;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void 사용자_정보와_권한을_Spring_Security_형식으로_제공한다() {
        User user = User.of(
                1L,
                "admin",
                "encoded-password",
                "관리자",
                Role.ROLE_ADMIN
        );
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly(Role.ROLE_ADMIN.name());
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }
}
