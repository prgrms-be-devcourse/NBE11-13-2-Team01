package com.example.delivery_project.dto.response;

import com.example.delivery_project.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 정보 응답")
public record UserInfoResponse(
        @Schema(description = "유저 id", example = "1")
        Long id,
        @Schema(description = "로그인 id", example = "driver1234")
        String loginId,
        @Schema(description = "유저 이름", example = "홍길동")
        String name,
        @Schema(description = "유저 권한", example = "ROLE_DELIVERY_DRIVER")
        Role role
) {
}
