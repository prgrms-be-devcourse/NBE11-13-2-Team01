package com.example.delivery_project.dto.request;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JoinRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        String name
) {
    public User toUser(String encodedPassword) {
        return User.of(
                loginId,
                encodedPassword,
                name
        );
    }
}
