package com.example.delivery_project.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateDeliveryPlanRequest(
        @NotBlank(message = "출발지 주소는 필수입니다.")
        String departureAddress,

        @NotNull(message = "예정 출발 시각은 필수입니다.")
        LocalDateTime scheduledDepartureAt,

        @NotEmpty(message = "배송지는 한 곳 이상이어야 합니다.")
        List<@Valid CreateDeliveryStopRequest> stops
) {
    public CreateDeliveryPlanRequest {
        stops = stops == null ? List.of() : List.copyOf(stops);
    }
}
