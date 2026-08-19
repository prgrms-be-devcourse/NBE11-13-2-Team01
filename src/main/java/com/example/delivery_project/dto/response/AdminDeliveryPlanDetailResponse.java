package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;

public record AdminDeliveryPlanDetailResponse(
        Long driverId,
        String driverLoginId,
        String driverName,
        DeliveryPlanDetailResponse deliveryPlan
) {
    public static AdminDeliveryPlanDetailResponse from(DeliveryPlan plan) {
        return new AdminDeliveryPlanDetailResponse(
                plan.getDriver().getId(),
                plan.getDriver().getLoginId(),
                plan.getDriver().getName(),
                DeliveryPlanDetailResponse.from(plan)
        );
    }
}
