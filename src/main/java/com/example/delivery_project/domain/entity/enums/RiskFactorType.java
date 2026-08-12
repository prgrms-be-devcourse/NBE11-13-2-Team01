package com.example.delivery_project.domain.entity.enums;

import lombok.Getter;

@Getter
public enum RiskFactorType {
    HEAVY_RAIN("폭우"),
    HEAT_WAVE("폭염"),
    WEATHER_WARNING("기상 특보");

    private final String description;
    RiskFactorType(String description) {
        this.description = description;
    }
}
