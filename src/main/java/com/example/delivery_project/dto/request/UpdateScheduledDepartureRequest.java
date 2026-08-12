package com.example.delivery_project.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateScheduledDepartureRequest(
        @NotNull LocalDateTime scheduledDepartureAt
) {
}
