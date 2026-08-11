package com.example.delivery_project.domain.entity.enums;

public enum DeliveryStopStatus {
    READY,
    DELIVERING,
    COMPLETED;

    public boolean isDelivering() {
        return this.equals(DELIVERING);
    }

    public boolean isCompleted() {
        return this.equals(COMPLETED);
    }
}
