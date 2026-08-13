package com.example.delivery_project.dto.request;

import com.example.delivery_project.spec.Location;

import java.time.LocalDateTime;
import java.util.List;

public record CreateDeliveryPlanRequest(
        String departureAddress,
        Double departureLatitude,
        Double departureLongitude,
        LocalDateTime scheduledDepartureAt,
        List<CreateDeliveryStopRequest> stops
) {
    public CreateDeliveryPlanRequest {
        stops = stops == null? List.of() : List.copyOf(stops);
    }

    public Location toDepartureLocation() {
        return new Location(
                departureAddress,
                departureLatitude,
                departureLongitude
        );
    }
}