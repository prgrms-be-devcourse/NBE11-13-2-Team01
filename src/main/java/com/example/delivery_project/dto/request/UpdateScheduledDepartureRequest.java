package com.example.delivery_project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "배송 예상 출발 시각 업데이트 요청")
public record UpdateScheduledDepartureRequest(
        @Schema(description = "배송 예상 출발 시각", example = "2026-01-01T00:00:00")
        @NotNull LocalDateTime scheduledDepartureAt
) {
}
