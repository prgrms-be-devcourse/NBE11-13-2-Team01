package com.example.delivery_project.dto.response;

public record LoginResponse(
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
