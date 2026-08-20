package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.dto.response.AdminDeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.AdminDeliveryPlanSummaryResponse;
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
class AdminDeliveryPlanServiceTest {

    @Mock
    private DeliveryPlanRepository deliveryPlanRepository;

    @InjectMocks
    private AdminDeliveryPlanService service;

    private DeliveryPlan plan;

    @BeforeEach
    void setUp() {
        User driver = User.of(
                7L,
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
        plan.addStop("서울시청", 37.56, 126.97, LocalDateTime.now());
    }

    @Test
    void 전체_배송계획에_담당기사_정보를_포함해_반환한다() {
        when(deliveryPlanRepository.findAllByOrderByScheduledDepartureAtAsc())
                .thenReturn(List.of(plan));

        List<AdminDeliveryPlanSummaryResponse> responses =
                service.getAllDeliveryPlans();

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.planId()).isEqualTo(10L);
            assertThat(response.driverId()).isEqualTo(7L);
            assertThat(response.driverLoginId()).isEqualTo("driver");
            assertThat(response.driverName()).isEqualTo("배송기사");
            assertThat(response.totalStops()).isEqualTo(1);
            assertThat(response.totalBoxes()).isZero();
            assertThat(response.remainingBoxes()).isZero();
        });
    }

    @Test
    void 관리자는_소유자와_무관하게_배송계획_상세를_조회한다() {
        when(deliveryPlanRepository.findDetailById(10L))
                .thenReturn(Optional.of(plan));

        AdminDeliveryPlanDetailResponse response =
                service.getDeliveryPlan(10L);

        assertThat(response.driverId()).isEqualTo(7L);
        assertThat(response.deliveryPlan().planId()).isEqualTo(10L);
        assertThat(response.deliveryPlan().deliveryStops()).hasSize(1);
    }

    @Test
    void 존재하지_않는_배송계획은_조회할_수_없다() {
        when(deliveryPlanRepository.findDetailById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDeliveryPlan(999L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(DeliveryException.DELIVERY_PLAN_NOT_FOUND)
                );
    }
}
