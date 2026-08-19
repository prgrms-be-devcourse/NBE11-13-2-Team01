package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.RiskFactor;
import com.example.delivery_project.enums.RiskFactorType;

public record RiskFactorResponse(
        RiskFactorType type,
        String description,
        int score
) {
    public static RiskFactorResponse from(RiskFactor factor) {
        return new RiskFactorResponse(
                factor.getType(),
                factor.getDescription(),
                factor.getRiskScore()
        );
    }
}
