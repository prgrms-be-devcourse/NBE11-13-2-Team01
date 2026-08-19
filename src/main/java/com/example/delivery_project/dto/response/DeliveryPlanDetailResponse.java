package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.enums.DeliveryPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "배송 계획 상세 응답")
public record DeliveryPlanDetailResponse(
        @Schema(description = "배송 계획 id",example = "1")
        Long planId,
        @Schema(description = "출발 지점",example = "서울특별시 서초구 반포대로 45, 명정빌딩 4층")
        String departureLocation,
        @Schema(description = "출발 지점 위도",example = "37.5665")
        Double departureLatitude,
        @Schema(description = "출발 지점 경도",example = "126.9780")
        Double departureLongitude,
        @Schema(description = "예상 출발 시각", example = "2026-01-01T00:00:00")
        LocalDateTime scheduledDepartureAt,
        @Schema(description = "실제 출발 시각", example = "2026-01-01T00:00:00")
        LocalDateTime actualDepartureAt,
        @Schema(description = "배송 상태",example = "DELIVERING")
        DeliveryPlanStatus status,
        @Schema(description = "배송 완료 시각",example = "2026-01-01T00:00:00")
        LocalDateTime completedAt,
        @Schema(description = "배송지 목록")
        List<DeliveryStopResponse> deliveryStops
) {
    public static DeliveryPlanDetailResponse from(DeliveryPlan plan) {
        List<DeliveryStopResponse> deliveryStops = plan.getDeliveryStops().stream()
                .map(DeliveryStopResponse::from)
                .toList();

        return new DeliveryPlanDetailResponse(
                plan.getId(),
                plan.getDepartureLocation(),
                plan.getDepartureLatitude(),
                plan.getDepartureLongitude(),
                plan.getScheduledDepartureAt(),
                plan.getActualDepartureAt(),
                plan.getStatus(),
                plan.getCompletedAt(),
                deliveryStops
        );
    }
}
