package com.example.delivery_project.enums;

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

    public boolean isReady() {
        return this.equals(READY);
    }
}
