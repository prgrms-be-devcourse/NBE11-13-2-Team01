package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.enums.DeliveryPlanStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DeliveryPlanDetailResponse(
        Long planId,
        String departureLocation,
        Double departureLatitude,
        Double departureLongitude,
        LocalDateTime scheduledDepartureAt,
        LocalDateTime actualDepartureAt,
        DeliveryPlanStatus status,
        LocalDateTime completedAt,
        List<DeliveryStopResponse> deliveryStops
) {
    public static DeliveryPlanDetailResponse from(DeliveryPlan plan) {
        List<DeliveryStopResponse> deliveryStops = plan.getDeliveryStops().stream()
                .map(DeliveryStopResponse::from)
                .toList();

        return new DeliveryPlanDetailResponse(
                plan.getId(),
                plan.getDepartureLocation(),
                plan.getDepartureLatitude(),
                plan.getDepartureLongitude(),
                plan.getScheduledDepartureAt(),
                plan.getActualDepartureAt(),
                plan.getStatus(),
                plan.getCompletedAt(),
                deliveryStops
        );
    }
}
