package com.example.delivery_project.dto.request;

import com.example.delivery_project.enums.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateDeliveryItemRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        String productName,

        @NotNull(message = "상품 유형은 필수입니다.")
        ProductType productType,

        @NotNull(message = "상품 수량은 필수입니다.")
        @Positive(message = "상품 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {
}
