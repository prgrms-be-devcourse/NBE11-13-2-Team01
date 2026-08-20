package com.example.delivery_project.dto.response;

import com.example.delivery_project.dto.projection.DeliveryPlanSummaryProjection;
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
        LocalDateTime completedAt,
        DeliveryPlanStatus status,
        int totalStops,
        long remainingStops,
        long totalBoxes,
        long remainingBoxes,
        long dangerStops
) {
    public static AdminDeliveryPlanSummaryResponse from(
            DeliveryPlanSummaryProjection summary
    ) {
        return new AdminDeliveryPlanSummaryResponse(
                summary.getPlanId(),
                summary.getDriverId(),
                summary.getDriverLoginId(),
                summary.getDriverName(),
                summary.getDepartureLocation(),
                summary.getScheduledDepartureAt(),
                summary.getActualDepartureAt(),
                summary.getCompletedAt(),
                DeliveryPlanStatus.valueOf(summary.getStatus()),
                summary.getTotalStops().intValue(),
                summary.getRemainingStops().longValue(),
                summary.getTotalBoxes().longValue(),
                summary.getRemainingBoxes().longValue(),
                summary.getDangerStops().longValue()
        );
    }
}
