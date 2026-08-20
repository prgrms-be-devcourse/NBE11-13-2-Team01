package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.enums.DeliveryPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "배송 계획 요약 응답")
public record DeliveryPlanSummaryResponse(
        @Schema(description = "배송 계획 id", example = "1")
        Long planId,
        @Schema(description = "출발 지점",example = "서울특별시 서초구 반포대로 45, 명정빌딩 4층")
        String departureLocation,
        @Schema(description = "예상 출발 시각", example = "2026-01-01T00:00:00")
        LocalDateTime scheduledDepartureAt,
        @Schema(description = "실제 출발 시각", example = "2026-01-01T00:00:00")
        LocalDateTime actualDepartureAt,
        @Schema(description = "배송 완료 시각", example = "2026-01-01T03:00:00")
        LocalDateTime completedAt,
        @Schema(description = "배송 상태", example = "DELIVERING")
        DeliveryPlanStatus status,
        @Schema(description = "총 배송지 수", example = "1")
        int totalStops,
        @Schema(description = "남은 배송지 수", example = "1")
        long remainingStops,
        @Schema(description = "전체 배송 물량(박스 수)", example = "12")
        long totalBoxes,
        @Schema(description = "남은 배송 물량(박스 수)", example = "7")
        long remainingBoxes,
        @Schema(description = "위험 배송지 수", example = "1")
        long dangerStops
) {
    public static DeliveryPlanSummaryResponse from(DeliveryPlan plan) {
        return new DeliveryPlanSummaryResponse(
                plan.getId(),
                plan.getDepartureLocation(),
                plan.getScheduledDepartureAt(),
                plan.getActualDepartureAt(),
                plan.getCompletedAt(),
                plan.getStatus(),
                plan.getTotalStops(),
                plan.getRemainingStops(),
                plan.getTotalBoxes(),
                plan.getRemainingBoxes(),
                plan.getDangerStops()
        );
    }
}
