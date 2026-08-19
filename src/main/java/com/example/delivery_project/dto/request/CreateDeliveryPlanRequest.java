package com.example.delivery_project.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "배송 계획 생성 요청")
public record CreateDeliveryPlanRequest(
        @Schema(description = "배송", example = "서울특별시 서초구 반포대로 45, 명정빌딩 4층")
        String departureAddress,

        @Schema(description = "예상 출발 시각", example = "2026-01-01T00:00:00")
        LocalDateTime scheduledDepartureAt,

        @Schema(description = "배송지 리스트")
        List<CreateDeliveryStopRequest> stops
) {
    public CreateDeliveryPlanRequest {
        stops = stops == null? List.of() : List.copyOf(stops);
    }
}