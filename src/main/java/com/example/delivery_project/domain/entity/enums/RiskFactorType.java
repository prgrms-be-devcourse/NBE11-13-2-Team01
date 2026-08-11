package com.example.delivery_project.domain.entity.enums;

import lombok.Getter;

@Getter
public enum RiskFactorType {
    HEAVY_RAIN("폭우"),
    HEAT_WAVE("폭염"),
    REFRIGERATED("냉장상품 포함"),
    FROZEN("냉동상품 포함"),
    WEATHER_WARNING("날씨 경보");

    private final String description;
    RiskFactorType(String description) {
        this.description = description;
    }
}
