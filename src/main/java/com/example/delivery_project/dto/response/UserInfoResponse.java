package com.example.delivery_project.dto.response;

import com.example.delivery_project.enums.Role;

public record UserInfoResponse(
        Long id,
        String loginId,
        String name,
        Role role
) {
}
