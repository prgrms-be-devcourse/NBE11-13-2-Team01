package com.example.delivery_project.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RiskLevelTest {

    @ParameterizedTest
    @CsvSource({
            "-1, UNKNOWN",
            "0, SAFE",
            "39, SAFE",
            "40, CAUTION",
            "69, CAUTION",
            "70, DANGER"
    })
    void 점수_경계값에_따라_위험등급을_반환한다(
            int score,
            RiskLevel expected
    ) {
        assertThat(RiskLevel.from(score)).isEqualTo(expected);
    }
}
