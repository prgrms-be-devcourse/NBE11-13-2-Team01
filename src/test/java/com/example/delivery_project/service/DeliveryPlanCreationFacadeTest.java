package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.request.CreateDeliveryItemRequest;
import com.example.delivery_project.dto.request.CreateDeliveryPlanRequest;
import com.example.delivery_project.dto.request.CreateDeliveryStopRequest;
import com.example.delivery_project.dto.response.WeatherRiskFactorResponse;
import com.example.delivery_project.dto.response.WeatherRiskResponse;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.exception.ExceptionCode;
import com.example.delivery_project.exception.global.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanCreationFacadeTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeliveryPlanRepository deliveryPlanRepository;

    private DeliveryPlanCreationFacade facade;

    @BeforeEach
    void setUp() {
        facade = new DeliveryPlanCreationFacade(userRepository, deliveryPlanRepository);
    }

    @Test
    void request_and_weather_response_are_mapped_and_saved_as_one_aggregate() {
        User driver = User.of("driver", "password", "배송 기사");
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(deliveryPlanRepository.save(any(DeliveryPlan.class))).thenAnswer(invocation -> {
            DeliveryPlan plan = invocation.getArgument(0);
            ReflectionTestUtils.setField(plan, "id", 100L);
            return plan;
        });

        CreateDeliveryPlanRequest request = new CreateDeliveryPlanRequest(
                "서울 물류센터",
                37.5,
                127.0,
                LocalDateTime.of(2026, 8, 14, 9, 0),
                List.of(new CreateDeliveryStopRequest(
                        "강남 고객지",
                        37.49,
                        127.03,
                        List.of(new CreateDeliveryItemRequest("생수", ProductType.NORMAL, 2))
                ))
        );
        WeatherRiskResponse weatherRisk = new WeatherRiskResponse(List.of(
                new WeatherRiskFactorResponse(RiskFactorType.HEAVY_RAIN, "폭우"),
                new WeatherRiskFactorResponse(RiskFactorType.WEATHER_WARNING, "호우 특보")
        ));

        Long planId = facade.create(1L, request, weatherRisk);

        assertThat(planId).isEqualTo(100L);
        ArgumentCaptor<DeliveryPlan> captor = ArgumentCaptor.forClass(DeliveryPlan.class);
        verify(deliveryPlanRepository).save(captor.capture());

        DeliveryPlan plan = captor.getValue();
        assertThat(plan.getDriver()).isSameAs(driver);
        assertThat(plan.getDepartureLocation()).isEqualTo("서울 물류센터");
        assertThat(plan.getDeliveryStops()).singleElement().satisfies(stop -> {
            assertThat(stop.getAddress()).isEqualTo("강남 고객지");
            assertThat(stop.getDeliveryItems()).singleElement().satisfies(item -> {
                assertThat(item.getProductName()).isEqualTo("생수");
                assertThat(item.getQuantity()).isEqualTo(2);
            });
            assertThat(stop.getRiskAssessment().getRiskFactors()).hasSize(2);
            assertThat(stop.getRiskAssessment().getLevel()).isEqualTo(RiskLevel.DANGER);
        });
    }

    @Test
    void plan_without_stops_is_saved() {
        User driver = User.of("driver", "password", "배송 기사");
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(deliveryPlanRepository.save(any(DeliveryPlan.class))).thenAnswer(invocation -> {
            DeliveryPlan plan = invocation.getArgument(0);
            ReflectionTestUtils.setField(plan, "id", 101L);
            return plan;
        });

        CreateDeliveryPlanRequest request = new CreateDeliveryPlanRequest(
                "출발지",
                37.5,
                127.0,
                LocalDateTime.of(2026, 8, 14, 9, 0),
                null
        );

        Long planId = facade.create(1L, request, new WeatherRiskResponse(List.of()));

        assertThat(planId).isEqualTo(101L);
        ArgumentCaptor<DeliveryPlan> captor = ArgumentCaptor.forClass(DeliveryPlan.class);
        verify(deliveryPlanRepository).save(captor.capture());
        assertThat(captor.getValue().getDeliveryStops()).isEmpty();
    }

    @Test
    void unknown_driver_rejects_creation_before_save() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        CreateDeliveryPlanRequest request = new CreateDeliveryPlanRequest(
                "출발지",
                37.5,
                127.0,
                LocalDateTime.of(2026, 8, 14, 9, 0),
                List.of()
        );

        assertThatThrownBy(() -> facade.create(
                999L,
                request,
                new WeatherRiskResponse(List.of())
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(ExceptionCode.INVALID_INPUT);
            assertThat(exception.getReason()).contains("999");
        });
        verify(deliveryPlanRepository, never()).save(any());
    }
}
