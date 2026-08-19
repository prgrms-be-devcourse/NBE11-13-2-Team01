package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

public record RiskAssessmentResponse(
        int score,
        RiskLevel level,
        LocalDateTime analyzedAt,
        List<RiskFactorResponse> factors
) {
    public static RiskAssessmentResponse from(
            RiskAssessment assessment
    ) {
        List<RiskFactorResponse> factors = assessment.getRiskFactors().stream()
                .map(RiskFactorResponse::from)
                .toList();

        return new RiskAssessmentResponse(
                assessment.getScore(),
                assessment.getLevel(),
                assessment.getAnalyzedAt(),
                factors
        );
    }
}
