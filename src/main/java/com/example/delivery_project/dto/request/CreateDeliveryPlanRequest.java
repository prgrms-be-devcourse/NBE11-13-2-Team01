package com.example.delivery_project.dto.request;
import java.time.LocalDateTime;
import java.util.List;

public record CreateDeliveryPlanRequest(
        String departureAddress,
        LocalDateTime scheduledDepartureAt,
        List<CreateDeliveryStopRequest> stops
) {
    public CreateDeliveryPlanRequest {
        stops = stops == null? List.of() : List.copyOf(stops);
    }
}