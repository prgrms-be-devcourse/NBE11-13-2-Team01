package com.example.delivery_project.dto.response;

import com.example.delivery_project.enums.RiskFactorType;

public record WeatherRiskFactorResponse(
        RiskFactorType type,
        String description
) {
}
