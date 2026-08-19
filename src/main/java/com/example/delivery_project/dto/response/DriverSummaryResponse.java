package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.user.User;

public record DriverSummaryResponse(
        Long driverId,
        String loginId,
        String name
) {
    public static DriverSummaryResponse from(User driver) {
        return new DriverSummaryResponse(
                driver.getId(),
                driver.getLoginId(),
                driver.getName()
        );
    }
}
