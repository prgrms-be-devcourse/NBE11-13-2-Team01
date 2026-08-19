package com.example.delivery_project.dto.request;

import com.example.delivery_project.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "배송 상품 생성 요청")
public record CreateDeliveryItemRequest(
        @Schema(description = "상품명", example = "생수1L")
        @NotBlank(message = "상품명은 필수입니다.")
        String productName,

        @Schema(description = "상품 종류", example = "FRAGILE")

        @NotNull(message = "상품 유형은 필수입니다.")
        ProductType productType,

        @Schema(description = "상품 개수",example = "10")

        @NotNull(message = "상품 수량은 필수입니다.")
        @Positive(message = "상품 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {
}
