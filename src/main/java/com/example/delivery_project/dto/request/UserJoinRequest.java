package com.example.delivery_project.dto.request;

import com.example.delivery_project.domain.entity.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "유저 회원가입 요청")
public record UserJoinRequest(
        @Schema(description = "로그인 아이디", example = "driver1234")
        @NotBlank(message = "아이디는 필수입니다.")
        String loginId,

        @Schema(description = "비밀번호", example = "pw1234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @Schema(description = "유저 이름", example = "홍길동")
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
