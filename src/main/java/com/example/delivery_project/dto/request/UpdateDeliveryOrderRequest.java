package com.example.delivery_project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "배송 순서 업데이트 요청")
public record UpdateDeliveryOrderRequest(
        @Schema(description = "순서를 변경할 배송지 id 목록")
        List<Long> stopIds
) {
}
