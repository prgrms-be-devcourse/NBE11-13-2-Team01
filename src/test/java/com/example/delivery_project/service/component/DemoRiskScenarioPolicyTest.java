package com.example.delivery_project.service.component;

import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.domain.entity.delivery.RiskFactor;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DemoRiskScenarioPolicyTest {

    private final DemoRiskScenarioPolicy policy =
            new DemoRiskScenarioPolicy(true);

    @Test
    void 배송지_5곳에_드라마틱한_위험도_시나리오를_반복_적용한다() {
        assertScenario(1L, RiskLevel.SAFE, 0);
        assertScenario(2L, RiskLevel.SAFE, 20, RiskFactorType.HEAT_WAVE);
        assertScenario(3L, RiskLevel.CAUTION, 40, RiskFactorType.WEATHER_WARNING);
        assertScenario(
                4L,
                RiskLevel.DANGER,
                70,
                RiskFactorType.HEAVY_RAIN,
                RiskFactorType.WEATHER_WARNING
        );
        assertScenario(5L, RiskLevel.UNKNOWN, -1);
        assertScenario(6L, RiskLevel.SAFE, 0);
    }

    @Test
    void 비활성화하면_기존_위험도를_변경하지_않는다() {
        DemoRiskScenarioPolicy disabledPolicy =
                new DemoRiskScenarioPolicy(false);
        RiskAssessment assessment = RiskAssessment.of(
                null,
                LocalDateTime.now()
        );
        assessment.replaceFactors(
                java.util.List.of(RiskFactorType.WEATHER_WARNING),
                LocalDateTime.now()
        );

        boolean applied = disabledPolicy.applyIfEnabled(
                1L,
                assessment,
                LocalDateTime.now()
        );

        assertThat(applied).isFalse();
        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.CAUTION);
        assertThat(assessment.getScore()).isEqualTo(40);
    }

    private void assertScenario(
            Long stopId,
            RiskLevel expectedLevel,
            int expectedScore,
            RiskFactorType... expectedFactorTypes
    ) {
        RiskAssessment assessment = RiskAssessment.of(
                null,
                LocalDateTime.now()
        );

        boolean applied = policy.applyIfEnabled(
                stopId,
                assessment,
                LocalDateTime.now()
        );

        assertThat(applied).isTrue();
        assertThat(assessment.getLevel()).isEqualTo(expectedLevel);
        assertThat(assessment.getScore()).isEqualTo(expectedScore);
        assertThat(assessment.getRiskFactors())
                .extracting(RiskFactor::getType)
                .containsExactly(expectedFactorTypes);
    }
}
