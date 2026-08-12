package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.enums.DeliveryStopStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DeliveryStopResponse(
        Long stopId,
        DeliveryStopStatus status,
        String address,
        Double latitude,
        Double longitude,
        LocalDateTime completedAt,
        List<DeliveryItemResponse> deliveryItems
) {
    public static DeliveryStopResponse from(DeliveryStop stop) {
        List<DeliveryItemResponse> deliveryItems = stop.getDeliveryItems().stream()
                .map(DeliveryItemResponse::from)
                .toList();
        return new DeliveryStopResponse(
                stop.getId(),
                stop.getStatus(),
                stop.getAddress(),
                stop.getLatitude(),
                stop.getLongitude(),
                stop.getCompletedAt(),
                deliveryItems
        );
    }
}
