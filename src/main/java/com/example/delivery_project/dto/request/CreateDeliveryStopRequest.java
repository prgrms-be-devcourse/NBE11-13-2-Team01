package com.example.delivery_project.dto.request;

import java.util.List;

public record CreateDeliveryStopRequest(
        String address,
        Double latitude,
        Double longitude,
        List<CreateDeliveryItemRequest> items
) {
    public CreateDeliveryStopRequest {
        items = items == null? List.of() : List.copyOf(items);
    }
}
