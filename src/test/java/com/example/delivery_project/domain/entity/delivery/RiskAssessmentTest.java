package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.spec.Location;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskAssessmentTest {

    @Test
    void assessment_starts_safe_and_owns_created_factors() {
        RiskAssessment assessment = assessment();

        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.SAFE);
        assertThat(assessment.getRiskFactors()).isEmpty();

        assessment.addFactor(RiskFactorType.WEATHER_WARNING, "기상 특보");

        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.CAUTION);
        assertThat(assessment.getRiskFactors()).singleElement().satisfies(factor -> {
            assertThat(factor.getRiskAssessment()).isSameAs(assessment);
            assertThat(factor.getType()).isEqualTo(RiskFactorType.WEATHER_WARNING);
            assertThat(factor.getDescription()).isEqualTo("기상 특보");
            assertThat(factor.getRiskScore()).isEqualTo(40);
        });
        assertThatThrownBy(() -> assessment.getRiskFactors().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private RiskAssessment assessment() {
        DeliveryPlan plan = DeliveryPlanFactory.create(
                User.of("driver", "password", "배송 기사"),
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
        return plan.addStop("목적지", 37.49, 127.03, LocalDateTime.now())
                .getRiskAssessment();
    }
}
