package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.DeliveryPlanStatus;
import com.example.delivery_project.enums.DeliveryStopStatus;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.spec.DeliveryItemSpec;
import com.example.delivery_project.spec.DeliveryStopSpec;
import com.example.delivery_project.spec.Location;
import com.example.delivery_project.spec.RiskFactorSpec;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryPlanTest {

    private static final Location DEPARTURE =
            new Location("서울 물류센터", 37.5665, 126.9780);

    private final User driver = User.of(
            1L,
            "driver",
            "encoded-password",
            "배송기사",
            Role.ROLE_DELIVERY_DRIVER
    );

    @Test
    void 팩토리는_배송지_상품_위험요인을_함께_생성한다() {
        DeliveryPlan plan = DeliveryPlanFactory.create(
                driver,
                DEPARTURE,
                LocalDateTime.now().plusHours(1),
                List.of(new DeliveryStopSpec(
                        new Location("서울시청", 37.5663, 126.9779),
                        List.of(new DeliveryItemSpec(
                                "냉동식품",
                                ProductType.FROZEN,
                                2
                        )),
                        List.of(
                                new RiskFactorSpec(
                                        RiskFactorType.HEAVY_RAIN,
                                        "폭우"
                                ),
                                new RiskFactorSpec(
                                        RiskFactorType.WEATHER_WARNING,
                                        "기상 특보"
                                )
                        )
                ))
        );

        DeliveryStop stop = plan.getDeliveryStops().getFirst();
        DeliveryItem item = stop.getDeliveryItems().getFirst();

        assertThat(plan.getStatus()).isEqualTo(DeliveryPlanStatus.READY);
        assertThat(plan.getTotalStops()).isEqualTo(1);
        assertThat(stop.getStatus()).isEqualTo(DeliveryStopStatus.READY);
        assertThat(item.getProductName()).isEqualTo("냉동식품");
        assertThat(item.getProductType()).isEqualTo(ProductType.FROZEN);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(stop.getRiskAssessment().getScore()).isEqualTo(70);
        assertThat(stop.getRiskAssessment().getLevel())
                .isEqualTo(RiskLevel.DANGER);
        assertThat(plan.getDangerStops()).isEqualTo(1);
    }

    @Test
    void 배송지가_없는_계획은_시작할_수_없다() {
        DeliveryPlan plan = emptyPlan();

        assertBusinessException(
                plan::start,
                DeliveryException.DELIVERY_PLAN_NOT_READY_TO_START
        );
    }

    @Test
    void 배송을_시작하면_계획과_모든_배송지가_DELIVERING이_된다() {
        DeliveryPlan plan = planWithTwoStops();

        plan.start();

        assertThat(plan.getStatus()).isEqualTo(DeliveryPlanStatus.DELIVERING);
        assertThat(plan.getActualDepartureAt()).isNotNull();
        assertThat(plan.getDeliveryStops())
                .allMatch(stop -> stop.getStatus() == DeliveryStopStatus.DELIVERING);
    }

    @Test
    void READY_상태에서만_배송_순서를_변경할_수_있다() {
        DeliveryPlan plan = planWithTwoStops();
        DeliveryStop first = plan.getDeliveryStops().get(0);
        DeliveryStop second = plan.getDeliveryStops().get(1);
        setId(first, 1L);
        setId(second, 2L);

        plan.reorderStops(List.of(2L, 1L));

        assertThat(plan.getDeliveryStops())
                .extracting(DeliveryStop::getId)
                .containsExactly(2L, 1L);

        plan.start();

        assertBusinessException(
                () -> plan.reorderStops(List.of(1L, 2L)),
                DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
        );
    }

    @Test
    void 일부_배송지를_누락한_순서_변경은_거부한다() {
        DeliveryPlan plan = planWithTwoStops();
        setId(plan.getDeliveryStops().get(0), 1L);
        setId(plan.getDeliveryStops().get(1), 2L);

        assertBusinessException(
                () -> plan.reorderStops(List.of(1L)),
                DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
        );
    }

    @Test
    void 모든_배송지를_완료해야_계획을_완료할_수_있다() {
        DeliveryPlan plan = planWithTwoStops();
        setId(plan.getDeliveryStops().get(0), 1L);
        setId(plan.getDeliveryStops().get(1), 2L);
        plan.start();

        plan.completeStop(1L);

        assertThat(plan.getRemainingStops()).isEqualTo(1);
        assertBusinessException(
                plan::finish,
                DeliveryException.DELIVERY_INCOMPLETE_STOP
        );

        plan.completeStop(2L);
        plan.finish();

        assertThat(plan.getStatus()).isEqualTo(DeliveryPlanStatus.COMPLETED);
        assertThat(plan.getRemainingStops()).isZero();
        assertThat(plan.getCompletedAt()).isNotNull();
        assertThat(plan.isFinished()).isTrue();
    }

    @Test
    void 배송_시작_후에는_예정시각과_상품을_변경할_수_없다() {
        DeliveryPlan plan = planWithTwoStops();
        DeliveryStop stop = plan.getDeliveryStops().getFirst();
        plan.start();

        assertBusinessException(
                () -> plan.updateScheduledDepartureAt(
                        LocalDateTime.now().plusDays(1)
                ),
                DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
        );
        assertBusinessException(
                () -> stop.addItem(
                        "추가 상품",
                        ProductType.NORMAL,
                        1
                ),
                DeliveryException.DELIVERY_INVALID_PLAN_STATUS_CHANGE
        );
    }

    @Test
    void 계획에_속하지_않은_배송지는_완료할_수_없다() {
        DeliveryPlan plan = planWithTwoStops();
        setId(plan.getDeliveryStops().get(0), 1L);
        setId(plan.getDeliveryStops().get(1), 2L);
        plan.start();

        assertBusinessException(
                () -> plan.completeStop(999L),
                DeliveryException.DELIVERY_STOP_NOT_FOUND
        );
    }

    private DeliveryPlan emptyPlan() {
        return DeliveryPlanFactory.create(
                driver,
                DEPARTURE,
                LocalDateTime.now().plusHours(1)
        );
    }

    private DeliveryPlan planWithTwoStops() {
        DeliveryPlan plan = emptyPlan();
        LocalDateTime analyzedAt = LocalDateTime.now();
        plan.addStop("배송지 1", 37.57, 126.98, analyzedAt);
        plan.addStop("배송지 2", 37.58, 126.99, analyzedAt);
        return plan;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }

    private void assertBusinessException(
            Runnable action,
            DeliveryException expected
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expected)
                );
    }
}
