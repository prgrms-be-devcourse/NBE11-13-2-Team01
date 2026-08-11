package com.example.delivery_project.domain.entity.delivery.spec;

import java.util.List;

public record DeliveryStopSpec(
        String address,
        Double latitude,
        Double longitude,
        List<DeliveryItemSpec> items
) {}
