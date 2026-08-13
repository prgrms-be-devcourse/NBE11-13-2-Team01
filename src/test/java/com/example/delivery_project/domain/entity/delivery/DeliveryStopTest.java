package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.spec.Location;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryStopTest {

    @Test
    void item_is_added_with_its_parent_stop() {
        DeliveryStop stop = newStop();

        DeliveryItem item = stop.addItem("냉동 식품", ProductType.FROZEN, 3);

        assertThat(stop.getDeliveryItems()).containsExactly(item);
        assertThat(item.getDeliveryStop()).isSameAs(stop);
        assertThat(item.getProductName()).isEqualTo("냉동 식품");
        assertThat(item.getProductType()).isEqualTo(ProductType.FROZEN);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThatThrownBy(() -> stop.getDeliveryItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void risk_level_is_recalculated_when_factors_are_added() {
        DeliveryStop stop = newStop();

        stop.addRiskFactor(RiskFactorType.HEAVY_RAIN, "폭우");
        assertThat(stop.getRiskAssessment().getLevel()).isEqualTo(RiskLevel.SAFE);

        stop.addRiskFactor(RiskFactorType.HEAT_WAVE, "폭염");
        assertThat(stop.getRiskAssessment().getLevel()).isEqualTo(RiskLevel.CAUTION);

        stop.addRiskFactor(RiskFactorType.WEATHER_WARNING, "기상 특보");
        assertThat(stop.getRiskAssessment().getLevel()).isEqualTo(RiskLevel.DANGER);
        assertThat(stop.isDangerStop()).isTrue();
    }

    private DeliveryStop newStop() {
        DeliveryPlan plan = DeliveryPlanFactory.create(
                User.of("driver", "password", "배송 기사"),
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
        return plan.addStop("목적지", 37.49, 127.03, LocalDateTime.now());
    }
}
