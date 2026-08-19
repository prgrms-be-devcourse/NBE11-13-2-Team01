package com.example.delivery_project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "배송 순서 업데이트 요청")
public record UpdateDeliveryOrderRequest(
        @Schema(description = "순서를 변경할 배송지 id 목록")
        @NotEmpty(message = "배송지 순서는 비어 있을 수 없습니다.")
        List<@NotNull(message = "배송지 ID는 필수입니다.") Long> stopIds
) {
}
