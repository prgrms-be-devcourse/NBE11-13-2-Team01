package com.example.delivery_project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 로그인 응답")
public record UserLoginResponse(
        @Schema(description = "액세스 토큰")
        String accessToken
        // TODO 프론트 작업하면서 response 형식 확정
        // 프론트에서 사용자 정보 사용할거면
        // A안) 수업처럼 User, refresh token도 응답에 넣고 컨트롤러에서 refresh token set NULL (레코드 setter 추가)
        // B안) 응답은 access token만, 사용자 정보는 /api/users/me에서 조회

        // Long userId,
        // String loginId,
        // String name,
        // Role role
) {
}
