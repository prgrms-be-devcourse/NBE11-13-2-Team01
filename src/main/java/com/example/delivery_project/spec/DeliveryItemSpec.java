package com.example.delivery_project.spec;

import com.example.delivery_project.enums.ProductType;

public record DeliveryItemSpec(
        String productName,
        ProductType productType,
        Integer quantity
) {
}
