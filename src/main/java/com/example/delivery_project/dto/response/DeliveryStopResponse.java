package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.enums.DeliveryStopStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "배송지 응답")
public record DeliveryStopResponse(
        @Schema(description = "배송지 id", example = "1")
        Long stopId,
        @Schema(description = "배송 상태", example = "DELIVERING")
        DeliveryStopStatus status,
        @Schema(description = "배송지 주소", example = "서울특별시 서초구 반포대로 45, 명정빌딩 4층")
        String address,
        @Schema(description = "배송지 위도", example = "37.5665")
        Double latitude,
        @Schema(description = "배송지 경도", example = "126.9780")
        Double longitude,
        @Schema(description = "배송 완료 시각", example = "2026-01-01T00:00:00")
        LocalDateTime completedAt,
        @Schema(description = "배송 위험도")
        RiskAssessmentResponse riskAssessment,
        @Schema(description = "배송 상품 목록")
        List<DeliveryItemResponse> deliveryItems
) {
    public static DeliveryStopResponse from(DeliveryStop stop) {
        List<DeliveryItemResponse> deliveryItems = stop.getDeliveryItems().stream()
                .map(DeliveryItemResponse::from)
                .toList();
        return new DeliveryStopResponse(
                stop.getId(),
                stop.getStatus(),
                stop.getAddress(),
                stop.getLatitude(),
                stop.getLongitude(),
                stop.getCompletedAt(),
                RiskAssessmentResponse.from(stop.getRiskAssessment()),
                deliveryItems
        );
    }
}
