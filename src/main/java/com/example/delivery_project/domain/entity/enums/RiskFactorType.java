package com.example.delivery_project.domain.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RiskFactorType {
    HEAVY_RAIN("폭우", 30),
    HEAT_WAVE("폭염", 20),
    WEATHER_WARNING("기상 특보", 40);

    private final String description;
    private final int riskScore;
}
