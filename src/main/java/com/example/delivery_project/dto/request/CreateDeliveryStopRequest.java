package com.example.delivery_project.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateDeliveryStopRequest(
        @NotBlank(message = "배송지 주소는 필수입니다.")
        String address,

        @NotEmpty(message = "배송 상품은 한 개 이상이어야 합니다.")
        List<@Valid CreateDeliveryItemRequest> items
) {
    public CreateDeliveryStopRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
