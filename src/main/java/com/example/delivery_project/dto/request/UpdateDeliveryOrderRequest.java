package com.example.delivery_project.dto.request;

import java.util.List;

public record UpdateDeliveryOrderRequest(
        List<Long> stopIds
) {
}
