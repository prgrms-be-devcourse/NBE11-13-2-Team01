package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.dto.request.UpdateDeliveryOrderRequest;
import com.example.delivery_project.dto.request.UpdateScheduledDepartureRequest;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanServiceTest {

    @Mock
    private DeliveryPlanRepository deliveryPlanRepository;

    @Mock
    private DeliveryStopRepository deliveryStopRepository;

    @Mock
    private DeliveryPlan plan;

    @Mock
    private DeliveryStop stop;

    private DeliveryPlanService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryPlanService(deliveryPlanRepository, deliveryStopRepository);
    }

    @Test
    void plan_list_is_mapped_to_summary_responses() {
        when(deliveryPlanRepository.findAllByDriverId(1L)).thenReturn(List.of(plan));
        when(plan.getTotalStops()).thenReturn(2);
        when(plan.getRemainingStops()).thenReturn(1L);
        when(plan.getDangerStops()).thenReturn(1L);

        var responses = service.getDeliveryPlans(1L);

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.totalStops()).isEqualTo(2);
            assertThat(response.remainingStops()).isEqualTo(1);
            assertThat(response.dangerStops()).isEqualTo(1);
        });
    }

    @Test
    void delivery_stop_is_loaded_with_plan_scope_and_mapped() {
        when(deliveryStopRepository.findDetailByIdAndPlanId(2L, 1L))
                .thenReturn(Optional.of(stop));
        when(stop.getDeliveryItems()).thenReturn(List.of());

        var response = service.getDeliveryStop(1L, 2L);

        assertThat(response.deliveryItems()).isEmpty();
        verify(deliveryStopRepository).findDetailByIdAndPlanId(2L, 1L);
    }

    @Test
    void missing_plan_and_stop_are_reported_with_domain_codes() {
        when(deliveryPlanRepository.findById(999L)).thenReturn(Optional.empty());
        when(deliveryStopRepository.findDetailByIdAndPlanId(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDeliveryPlan(999L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(DeliveryException.DELIVERY_PLAN_NOT_FOUND));
        assertThatThrownBy(() -> service.getDeliveryStop(1L, 999L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(DeliveryException.DELIVERY_STOP_NOT_FOUND));
    }

    @Test
    void mutation_use_cases_delegate_to_the_aggregate_root() {
        when(deliveryPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        LocalDateTime changedAt = LocalDateTime.of(2026, 8, 15, 10, 0);

        service.changeScheduledDepartureAt(
                1L,
                new UpdateScheduledDepartureRequest(changedAt)
        );
        service.reorderStops(1L, new UpdateDeliveryOrderRequest(List.of(2L, 1L)));
        service.start(1L);
        service.completeStop(1L, 2L);
        service.completePlan(1L);

        verify(plan).updateScheduledDepartureAt(changedAt);
        verify(plan).reorderStops(List.of(2L, 1L));
        verify(plan).start();
        verify(plan).completeStop(2L);
        verify(plan).finish();
    }
}
