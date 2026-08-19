package com.example.delivery_project.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateDeliveryOrderRequest(
        @NotEmpty(message = "배송지 순서는 비어 있을 수 없습니다.")
        List<@NotNull(message = "배송지 ID는 필수입니다.") Long> stopIds
) {
}
