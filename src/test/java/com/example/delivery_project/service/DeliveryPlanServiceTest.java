package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.dto.request.UpdateDeliveryOrderRequest;
import com.example.delivery_project.dto.request.UpdateScheduledDepartureRequest;
import com.example.delivery_project.dto.response.DeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.DeliveryPlanSummaryResponse;
import com.example.delivery_project.dto.response.DeliveryStopResponse;
import com.example.delivery_project.enums.DeliveryPlanStatus;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.spec.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanServiceTest {

    @Mock
    private DeliveryPlanRepository deliveryPlanRepository;

    @Mock
    private DeliveryStopRepository deliveryStopRepository;

    @InjectMocks
    private DeliveryPlanService deliveryPlanService;

    private DeliveryPlan plan;
    private DeliveryStop firstStop;
    private DeliveryStop secondStop;

    @BeforeEach
    void setUp() {
        User driver = User.of(
                1L,
                "driver",
                "password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
        plan = DeliveryPlanFactory.create(
                driver,
                new Location("서울 물류센터", 37.50, 126.90),
                LocalDateTime.now().plusHours(1)
        );
        ReflectionTestUtils.setField(plan, "id", 10L);

        firstStop = plan.addStop(
                "배송지 1",
                37.51,
                126.91,
                LocalDateTime.now()
        );
        secondStop = plan.addStop(
                "배송지 2",
                37.52,
                126.92,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(firstStop, "id", 101L);
        ReflectionTestUtils.setField(secondStop, "id", 102L);
        firstStop.addItem("상품", ProductType.NORMAL, 2);
    }

    @Test
    void 기사별_배송계획_목록을_요약_응답으로_반환한다() {
        when(deliveryPlanRepository.findAllByDriverId(1L))
                .thenReturn(List.of(plan));

        List<DeliveryPlanSummaryResponse> responses =
                deliveryPlanService.getDeliveryPlans(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().planId()).isEqualTo(10L);
        assertThat(responses.getFirst().totalStops()).isEqualTo(2);
        assertThat(responses.getFirst().remainingStops()).isEqualTo(2);
    }

    @Test
    void 배송계획과_배송지_상세를_응답으로_변환한다() {
        when(deliveryPlanRepository.findByIdAndDriverId(10L, 1L))
                .thenReturn(Optional.of(plan));
        when(deliveryStopRepository.findDetailByIdAndPlanId(101L, 10L))
                .thenReturn(Optional.of(firstStop));

        DeliveryPlanDetailResponse planResponse =
                deliveryPlanService.getDeliveryPlan(10L, 1L);
        DeliveryStopResponse stopResponse =
                deliveryPlanService.getDeliveryStop(10L, 101L, 1L);

        assertThat(planResponse.planId()).isEqualTo(10L);
        assertThat(planResponse.deliveryStops()).hasSize(2);
        assertThat(stopResponse.stopId()).isEqualTo(101L);
        assertThat(stopResponse.deliveryItems()).hasSize(1);
        assertThat(stopResponse.riskAssessment().score()).isEqualTo(-1);
        assertThat(stopResponse.riskAssessment().level())
                .isEqualTo(RiskLevel.UNKNOWN);
        assertThat(stopResponse.riskAssessment().factors()).isEmpty();
    }

    @Test
    void 배송_상태_변경_흐름을_서비스에서_수행한다() {
        when(deliveryPlanRepository.findByIdAndDriverId(10L, 1L))
                .thenReturn(Optional.of(plan));
        LocalDateTime changedDepartureAt = LocalDateTime.now().plusHours(2);

        deliveryPlanService.changeScheduledDepartureAt(
                10L,
                1L,
                new UpdateScheduledDepartureRequest(changedDepartureAt)
        );
        deliveryPlanService.reorderStops(
                10L,
                1L,
                new UpdateDeliveryOrderRequest(List.of(102L, 101L))
        );
        deliveryPlanService.start(10L, 1L);
        deliveryPlanService.completeStop(10L, 101L, 1L);
        deliveryPlanService.completeStop(10L, 102L, 1L);
        deliveryPlanService.completePlan(10L, 1L);

        assertThat(plan.getScheduledDepartureAt())
                .isEqualTo(changedDepartureAt);
        assertThat(plan.getDeliveryStops())
                .extracting(DeliveryStop::getId)
                .containsExactly(102L, 101L);
        assertThat(plan.getStatus())
                .isEqualTo(DeliveryPlanStatus.COMPLETED);
    }

    @Test
    void 존재하지_않는_계획을_조회하면_예외가_발생한다() {
        when(deliveryPlanRepository.findByIdAndDriverId(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryPlanService.getDeliveryPlan(999L, 1L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(DeliveryException.DELIVERY_PLAN_NOT_FOUND)
                );
    }

    @Test
    void 계획에_속한_배송지가_없으면_예외가_발생한다() {
        when(deliveryPlanRepository.findByIdAndDriverId(10L, 1L))
                .thenReturn(Optional.of(plan));
        when(deliveryStopRepository.findDetailByIdAndPlanId(999L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> deliveryPlanService.getDeliveryStop(10L, 999L, 1L)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(DeliveryException.DELIVERY_STOP_NOT_FOUND)
        );
    }

    @Test
    void 다른_기사의_배송계획에는_접근할_수_없다() {
        when(deliveryPlanRepository.findByIdAndDriverId(10L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> deliveryPlanService.getDeliveryPlan(10L, 2L)
        ).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(DeliveryException.DELIVERY_PLAN_NOT_FOUND)
        );
    }
}
