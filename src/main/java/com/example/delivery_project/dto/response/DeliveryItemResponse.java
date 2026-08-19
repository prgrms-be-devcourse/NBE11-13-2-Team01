package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryItem;
import com.example.delivery_project.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "배송 상품 응답")
public record DeliveryItemResponse(
        @Schema(description = "상품 id")
        Long itemId,
        @Schema(description = "상품명", example = "생수1L")
        String productName,
        @Schema(description = "상품 종류", example = "FRAGILE")
        ProductType productType,
        @Schema(description = "상품 개수",example = "10")
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
