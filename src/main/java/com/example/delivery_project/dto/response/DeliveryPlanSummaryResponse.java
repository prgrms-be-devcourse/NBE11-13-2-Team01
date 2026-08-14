package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.enums.DeliveryPlanStatus;

import java.time.LocalDateTime;

public record DeliveryPlanSummaryResponse(
        Long planId,
        String departureLocation,
        LocalDateTime scheduledDepartureAt,
        LocalDateTime actualDepartureAt,
        DeliveryPlanStatus status,
        int totalStops,
        long remainingStops,
        long dangerStops
) {
    public static DeliveryPlanSummaryResponse from(DeliveryPlan plan) {
        return new DeliveryPlanSummaryResponse(
                plan.getId(),
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
