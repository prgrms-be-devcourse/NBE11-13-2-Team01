package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskAssessmentTest {

    @Test
    void 날씨_분석_전에는_UNKNOWN과_마이너스_1점을_반환한다() {
        RiskAssessment assessment = RiskAssessment.of(
                null,
                LocalDateTime.now()
        );

        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.UNKNOWN);
        assertThat(assessment.getScore()).isEqualTo(-1);
    }

    @Test
    void 날씨_분석에_성공하면_위험_요인으로_점수와_등급을_계산한다() {
        RiskAssessment assessment = RiskAssessment.of(
                null,
                LocalDateTime.now()
        );

        assessment.replaceFactors(
                List.of(
                        RiskFactorType.HEAVY_RAIN,
                        RiskFactorType.HEAT_WAVE
                ),
                LocalDateTime.now()
        );

        assertThat(assessment.getScore()).isEqualTo(50);
        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.CAUTION);
    }

    @Test
    void 사용_가능한_날씨가_없으면_기존_요인을_지우고_UNKNOWN으로_변경한다() {
        RiskAssessment assessment = RiskAssessment.of(
                null,
                LocalDateTime.now()
        );
        assessment.replaceFactors(
                List.of(RiskFactorType.HEAVY_RAIN),
                LocalDateTime.now()
        );

        assessment.markUnknown(LocalDateTime.now());

        assertThat(assessment.getRiskFactors()).isEmpty();
        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.UNKNOWN);
        assertThat(assessment.getScore()).isEqualTo(-1);
    }
}
