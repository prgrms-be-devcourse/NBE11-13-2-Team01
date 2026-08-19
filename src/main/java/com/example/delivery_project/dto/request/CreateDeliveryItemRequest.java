package com.example.delivery_project.dto.request;

import com.example.delivery_project.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "배송 상품 생성 요청")
public record CreateDeliveryItemRequest(
        @Schema(description = "상품명", example = "생수1L")
        String productName,

        @Schema(description = "상품 종류", example = "FRAGILE")
        ProductType productType,

        @Schema(description = "상품 개수",example = "10")
        Integer quantity
) {
}