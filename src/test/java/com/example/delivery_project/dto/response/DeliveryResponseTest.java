package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryItem;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.spec.Location;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryResponseTest {

    @Test
    void detail_response_maps_nested_stop_and_item() {
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 8, 14, 9, 0);
        DeliveryPlan plan = DeliveryPlanFactory.create(
                User.of("driver", "password", "배송 기사"),
                new Location("출발지", 37.5, 127.0),
                scheduledAt
        );
        DeliveryStop stop = plan.addStop("목적지", 37.49, 127.03, LocalDateTime.now());
        DeliveryItem item = stop.addItem("생수", ProductType.NORMAL, 2);
        ReflectionTestUtils.setField(plan, "id", 1L);
        ReflectionTestUtils.setField(stop, "id", 2L);
        ReflectionTestUtils.setField(item, "id", 3L);

        DeliveryPlanDetailResponse response = DeliveryPlanDetailResponse.from(plan);

        assertThat(response.planId()).isEqualTo(1L);
        assertThat(response.departureLocation()).isEqualTo("출발지");
        assertThat(response.scheduledDepartureAt()).isEqualTo(scheduledAt);
        assertThat(response.deliveryStops()).singleElement().satisfies(stopResponse -> {
            assertThat(stopResponse.stopId()).isEqualTo(2L);
            assertThat(stopResponse.address()).isEqualTo("목적지");
            assertThat(stopResponse.deliveryItems()).singleElement().satisfies(itemResponse -> {
                assertThat(itemResponse.itemId()).isEqualTo(3L);
                assertThat(itemResponse.productName()).isEqualTo("생수");
                assertThat(itemResponse.quantity()).isEqualTo(2);
            });
        });
    }

    @Test
    void summary_response_maps_derived_counts() {
        DeliveryPlan plan = DeliveryPlanFactory.create(
                User.of("driver", "password", "배송 기사"),
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
        plan.addStop("목적지", 37.49, 127.03, LocalDateTime.now());

        DeliveryPlanSummaryResponse response = DeliveryPlanSummaryResponse.from(plan);

        assertThat(response.totalStops()).isEqualTo(1);
        assertThat(response.remainingStops()).isEqualTo(1);
        assertThat(response.dangerStops()).isZero();
    }
}
