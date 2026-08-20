package com.example.delivery_project.service.component;

import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.enums.RiskFactorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DemoRiskScenarioPolicy {

    private static final int SCENARIO_SIZE = 5;

    private final boolean enabled;

    public DemoRiskScenarioPolicy(
            @Value("${demo.risk.enabled:false}") boolean enabled
    ) {
        this.enabled = enabled;
    }

    public boolean applyIfEnabled(
            Long stopId,
            RiskAssessment assessment,
            LocalDateTime analyzedAt
    ) {
        if (!enabled) {
            return false;
        }

        int scenarioIndex = Math.floorMod(
                stopId - 1,
                SCENARIO_SIZE
        );

        switch (scenarioIndex) {
            case 0 -> assessment.replaceFactors(
                    List.of(),
                    analyzedAt
            );
            case 1 -> assessment.replaceFactors(
                    List.of(RiskFactorType.HEAT_WAVE),
                    analyzedAt
            );
            case 2 -> assessment.replaceFactors(
                    List.of(RiskFactorType.WEATHER_WARNING),
                    analyzedAt
            );
            case 3 -> assessment.replaceFactors(
                    List.of(
                            RiskFactorType.HEAVY_RAIN,
                            RiskFactorType.WEATHER_WARNING
                    ),
                    analyzedAt
            );
            case 4 -> assessment.markUnknown(analyzedAt);
            default -> throw new IllegalStateException(
                    "지원하지 않는 데모 위험도 시나리오입니다."
            );
        }

        return true;
    }
}
