package com.example.delivery_project.dto.request;

import com.example.delivery_project.enums.ProductType;

public record CreateDeliveryItemRequest(
        String productName,
        ProductType productType,
        Integer quantity
) {
}