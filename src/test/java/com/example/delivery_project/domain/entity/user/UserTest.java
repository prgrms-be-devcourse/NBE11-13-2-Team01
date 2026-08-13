package com.example.delivery_project.domain.entity.user;

import com.example.delivery_project.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void default_factory_creates_delivery_driver() {
        User user = User.of("driver", "password", "배송 기사");

        assertThat(user.getLoginId()).isEqualTo("driver");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getName()).isEqualTo("배송 기사");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_DELIVERY_DRIVER);
    }

    @Test
    void explicit_role_is_preserved() {
        User user = User.of("admin", "password", "관리자", Role.ROLE_ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }
}
