package com.example.delivery_project.domain.entity.enums;

public enum DeliveryPlanStatus {
    READY,
    DELIVERING,
    COMPLETED;

    public boolean isReady() {
        return this.equals(READY);
    }

    public boolean isDelivering() {
        return this.equals(DELIVERING);
    }

    public boolean isCompleted() {
        return this.equals(COMPLETED);
    }
}
