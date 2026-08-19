package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.enums.DeliveryPlanStatus;

import java.time.LocalDateTime;

public record AdminDeliveryPlanSummaryResponse(
        Long planId,
        Long driverId,
        String driverLoginId,
        String driverName,
        String departureLocation,
        LocalDateTime scheduledDepartureAt,
        LocalDateTime actualDepartureAt,
        DeliveryPlanStatus status,
        int totalStops,
        long remainingStops,
        long dangerStops
) {
    public static AdminDeliveryPlanSummaryResponse from(DeliveryPlan plan) {
        return new AdminDeliveryPlanSummaryResponse(
                plan.getId(),
                plan.getDriver().getId(),
                plan.getDriver().getLoginId(),
                plan.getDriver().getName(),
                plan.getDepartureLocation(),
                plan.getScheduledDepartureAt(),
                plan.getActualDepartureAt(),
                plan.getStatus(),
                plan.getTotalStops(),
                plan.getRemainingStops(),
                plan.getDangerStops()
        );
    }
}
