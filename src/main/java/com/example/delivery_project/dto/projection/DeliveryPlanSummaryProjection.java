package com.example.delivery_project.dto.projection;

import java.time.LocalDateTime;

public interface DeliveryPlanSummaryProjection {
    Long getPlanId();
    Long getDriverId();
    String getDriverLoginId();
    String getDriverName();
    String getDepartureLocation();
    LocalDateTime getScheduledDepartureAt();
    LocalDateTime getActualDepartureAt();
    LocalDateTime getCompletedAt();
    String getStatus();
    Number getTotalStops();
    Number getRemainingStops();
    Number getTotalBoxes();
    Number getRemainingBoxes();
    Number getDangerStops();
}
