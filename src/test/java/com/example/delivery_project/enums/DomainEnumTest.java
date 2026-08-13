package com.example.delivery_project.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEnumTest {

    @Test
    void risk_level_uses_boundary_scores() {
        assertThat(RiskLevel.from(0)).isEqualTo(RiskLevel.SAFE);
        assertThat(RiskLevel.from(39)).isEqualTo(RiskLevel.SAFE);
        assertThat(RiskLevel.from(40)).isEqualTo(RiskLevel.CAUTION);
        assertThat(RiskLevel.from(69)).isEqualTo(RiskLevel.CAUTION);
        assertThat(RiskLevel.from(70)).isEqualTo(RiskLevel.DANGER);
    }

    @Test
    void status_helpers_match_only_their_status() {
        assertThat(DeliveryPlanStatus.READY.isReady()).isTrue();
        assertThat(DeliveryPlanStatus.DELIVERING.isDelivering()).isTrue();
        assertThat(DeliveryPlanStatus.COMPLETED.isCompleted()).isTrue();
        assertThat(DeliveryStopStatus.READY.isReady()).isTrue();
        assertThat(DeliveryStopStatus.DELIVERING.isDelivering()).isTrue();
        assertThat(DeliveryStopStatus.COMPLETED.isCompleted()).isTrue();
    }
}
