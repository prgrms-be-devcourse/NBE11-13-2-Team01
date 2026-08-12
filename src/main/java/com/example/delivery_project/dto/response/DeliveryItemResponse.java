package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryItem;
import com.example.delivery_project.domain.entity.enums.ProductType;

public record DeliveryItemResponse(
        Long itemId,
        String productName,
        ProductType productType,
        int quantity
){
    public static DeliveryItemResponse from(DeliveryItem item){
        return new DeliveryItemResponse(
                item.getId(),
                item.getProductName(),
                item.getProductType(),
                item.getQuantity()
        );
    }
}
