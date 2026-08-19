package com.example.delivery_project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "배송지 생성 요청")
public record CreateDeliveryStopRequest(
        @Schema(description = "배송주소", example = "서울특별시 서초구 반포대로 45, 명정빌딩 4층")
        String address,
        @Schema(description = "배송 상품 목록")
        List<CreateDeliveryItemRequest> items
) {
    public CreateDeliveryStopRequest {
        items = items == null? List.of() : List.copyOf(items);
    }
}
