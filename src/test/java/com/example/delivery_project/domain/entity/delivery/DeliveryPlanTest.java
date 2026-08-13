package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.DeliveryPlanStatus;
import com.example.delivery_project.enums.DeliveryStopStatus;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.spec.Location;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryPlanTest {

    @Test
    void adding_stop_creates_required_relationships_and_safe_assessment() {
        DeliveryPlan plan = emptyPlan();
        LocalDateTime analyzedAt = LocalDateTime.of(2026, 8, 13, 12, 0);

        DeliveryStop stop = plan.addStop(
                new Location("목적지", 37.49, 127.03),
                analyzedAt
        );

        assertThat(plan.getDeliveryStops()).containsExactly(stop);
        assertThat(stop.getDeliveryPlan()).isSameAs(plan);
        assertThat(stop.getStatus()).isEqualTo(DeliveryStopStatus.READY);
        assertThat(stop.getRiskAssessment().getDeliveryStop()).isSameAs(stop);
        assertThat(stop.getRiskAssessment().getAnalyzedAt()).isEqualTo(analyzedAt);
        assertThat(stop.isDangerStop()).isFalse();
    }

    @Test
    void empty_plan_cannot_start() {
        assertBusinessException(
                () -> emptyPlan().start(),
                DeliveryException.DELIVERY_PLAN_NOT_READY_TO_START
        );
    }

    @Test
    void plan_can_start_complete_stop_and_finish() {
        DeliveryPlan plan = emptyPlan();
        DeliveryStop stop = plan.addStop("목적지", 37.49, 127.03, LocalDateTime.now());
        ReflectionTestUtils.setField(stop, "id", 10L);

        plan.start();

        assertThat(plan.getStatus()).isEqualTo(DeliveryPlanStatus.DELIVERING);
        assertThat(plan.getActualDepartureAt()).isNotNull();
        assertThat(stop.getStatus()).isEqualTo(DeliveryStopStatus.DELIVERING);

        plan.completeStop(10L);
        plan.finish();

        assertThat(stop.getStatus()).isEqualTo(DeliveryStopStatus.COMPLETED);
        assertThat(stop.getCompletedAt()).isNotNull();
        assertThat(plan.getStatus()).isEqualTo(DeliveryPlanStatus.COMPLETED);
        assertThat(plan.getCompletedAt()).isNotNull();
        assertThat(plan.isFinished()).isTrue();
    }

    @Test
    void ready_only_mutations_are_rejected_after_start() {
        DeliveryPlan plan = emptyPlan();
        DeliveryStop stop = plan.addStop("목적지", 37.49, 127.03, LocalDateTime.now());
        plan.start();

        assertBusinessException(
                () -> plan.addStop("추가 목적지", 37.48, 127.04, LocalDateTime.now()),
                DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
        );
        assertBusinessException(
                () -> plan.updateScheduledDepartureAt(LocalDateTime.now().plusDays(1)),
                DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
        );
        assertBusinessException(
                () -> stop.addItem("생수", com.example.delivery_project.enums.ProductType.NORMAL, 1),
                DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
        );
    }

    @Test
    void stops_can_be_reordered_only_with_the_complete_existing_id_set() {
        DeliveryPlan plan = emptyPlan();
        DeliveryStop first = plan.addStop("첫 번째", 37.49, 127.03, LocalDateTime.now());
        DeliveryStop second = plan.addStop("두 번째", 37.48, 127.04, LocalDateTime.now());
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);

        plan.reorderStops(List.of(2L, 1L));

        assertThat(plan.getDeliveryStops()).containsExactly(second, first);

        assertBusinessException(
                () -> plan.reorderStops(List.of(2L)),
                DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
        );
    }

    @Test
    void unknown_stop_cannot_be_completed_and_collections_cannot_be_modified_externally() {
        DeliveryPlan plan = emptyPlan();
        DeliveryStop stop = plan.addStop("목적지", 37.49, 127.03, LocalDateTime.now());
        ReflectionTestUtils.setField(stop, "id", 1L);

        assertBusinessException(
                () -> plan.completeStop(999L),
                DeliveryException.DELIVERY_STOP_NOT_FOUND
        );
        assertThatThrownBy(() -> plan.getDeliveryStops().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private DeliveryPlan emptyPlan() {
        return DeliveryPlanFactory.create(
                User.of("driver", "password", "배송 기사"),
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
    }

    private void assertBusinessException(
            Runnable action,
            DeliveryException expectedCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode));
    }
}
