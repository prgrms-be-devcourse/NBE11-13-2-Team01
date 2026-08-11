package com.example.delivery_project.domain.entity.delivery.spec;

import com.example.delivery_project.domain.entity.enums.ProductType;

public record DeliveryItemSpec(
        String productName,
        ProductType productType,
        Integer quantity
) {
}
