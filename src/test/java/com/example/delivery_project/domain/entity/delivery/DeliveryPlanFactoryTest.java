package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.spec.DeliveryItemSpec;
import com.example.delivery_project.spec.DeliveryStopSpec;
import com.example.delivery_project.spec.Location;
import com.example.delivery_project.spec.RiskFactorSpec;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryPlanFactoryTest {

    @Test
    void plan_stop_item_assessment_factor_aggregate_is_created() {
        User driver = User.of("driver", "password", "배송 기사");
        Location departure = new Location("서울 물류센터", 37.50, 127.00);
        Location destination = new Location("강남 고객지", 37.49, 127.03);
        LocalDateTime scheduledDepartureAt = LocalDateTime.of(2026, 8, 14, 9, 0);

        DeliveryStopSpec stopSpec = new DeliveryStopSpec(
                destination,
                List.of(new DeliveryItemSpec("생수", ProductType.NORMAL, 2)),
                List.of(
                        new RiskFactorSpec(RiskFactorType.HEAVY_RAIN, "시간당 강수량 40mm"),
                        new RiskFactorSpec(RiskFactorType.WEATHER_WARNING, "호우 특보")
                )
        );

        DeliveryPlan plan = DeliveryPlanFactory.create(
                driver,
                departure,
                scheduledDepartureAt,
                List.of(stopSpec)
        );

        assertThat(plan.getDriver()).isSameAs(driver);
        assertThat(plan.getDepartureLocation()).isEqualTo("서울 물류센터");
        assertThat(plan.getScheduledDepartureAt()).isEqualTo(scheduledDepartureAt);
        assertThat(plan.getDeliveryStops()).hasSize(1);

        DeliveryStop stop = plan.getDeliveryStops().getFirst();
        assertThat(stop.getDeliveryPlan()).isSameAs(plan);
        assertThat(stop.getAddress()).isEqualTo("강남 고객지");
        assertThat(stop.getLatitude()).isEqualTo(37.49);
        assertThat(stop.getLongitude()).isEqualTo(127.03);

        DeliveryItem item = stop.getDeliveryItems().getFirst();
        assertThat(item.getDeliveryStop()).isSameAs(stop);
        assertThat(item.getProductName()).isEqualTo("생수");
        assertThat(item.getQuantity()).isEqualTo(2);

        RiskAssessment assessment = stop.getRiskAssessment();
        assertThat(assessment.getDeliveryStop()).isSameAs(stop);
        assertThat(assessment.getAnalyzedAt()).isNotNull();
        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.DANGER);
        assertThat(assessment.getRiskFactors()).hasSize(2)
                .allSatisfy(factor -> assertThat(factor.getRiskAssessment()).isSameAs(assessment));
    }

    @Test
    void plan_can_be_created_without_stops() {
        DeliveryPlan plan = DeliveryPlanFactory.create(
                User.of("driver", "password", "배송 기사"),
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );

        assertThat(plan.getDeliveryStops()).isEmpty();
        assertThat(plan.getTotalStops()).isZero();
        assertThat(plan.areAllStopsCompleted()).isFalse();
    }
}
