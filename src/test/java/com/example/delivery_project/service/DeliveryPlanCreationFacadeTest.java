package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.request.CreateDeliveryItemRequest;
import com.example.delivery_project.dto.request.CreateDeliveryPlanRequest;
import com.example.delivery_project.dto.request.CreateDeliveryStopRequest;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.event.DeliveryPlanCreatedEvent;
import com.example.delivery_project.exception.ExceptionCode;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.service.component.GeocodingClient;
import com.example.delivery_project.service.component.LocationMapper;
import com.example.delivery_project.spec.GeocodedLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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

    @Mock
    private GeocodingClient geocodingClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeliveryPlanCreationFacade facade;

    @BeforeEach
    void setUp() {
        facade = new DeliveryPlanCreationFacade(
                userRepository,
                deliveryPlanRepository,
                geocodingClient,
                new LocationMapper(),
                eventPublisher
        );
    }

    @Test
    void 주소를_좌표로_변환해_계획을_저장하고_생성_이벤트를_발행한다() {
        User driver = User.of(
                7L,
                "driver",
                "password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
        when(userRepository.findById(7L))
                .thenReturn(Optional.of(driver));
        when(geocodingClient.geocode("서울 물류센터"))
                .thenReturn(new GeocodedLocation(
                        "서울 물류센터",
                        37.50,
                        126.90
                ));
        when(geocodingClient.geocode("서울시청"))
                .thenReturn(new GeocodedLocation(
                        "서울시청",
                        37.5663,
                        126.9779
                ));
        when(deliveryPlanRepository.save(any(DeliveryPlan.class)))
                .thenAnswer(invocation -> {
                    DeliveryPlan plan = invocation.getArgument(0);
                    ReflectionTestUtils.setField(plan, "id", 100L);
                    return plan;
                });

        CreateDeliveryPlanRequest request =
                new CreateDeliveryPlanRequest(
                        "서울 물류센터",
                        LocalDateTime.now().plusHours(1),
                        List.of(new CreateDeliveryStopRequest(
                                "서울시청",
                                List.of(new CreateDeliveryItemRequest(
                                        "냉동식품",
                                        ProductType.FROZEN,
                                        3
                                ))
                        ))
                );

        Long planId = facade.create(7L, request);

        ArgumentCaptor<DeliveryPlan> planCaptor =
                ArgumentCaptor.forClass(DeliveryPlan.class);
        verify(deliveryPlanRepository).save(planCaptor.capture());

        DeliveryPlan savedPlan = planCaptor.getValue();
        DeliveryStop savedStop = savedPlan.getDeliveryStops().getFirst();

        assertThat(planId).isEqualTo(100L);
        assertThat(savedPlan.getDriver()).isEqualTo(driver);
        assertThat(savedPlan.getDepartureLocation())
                .isEqualTo("서울 물류센터");
        assertThat(savedPlan.getDepartureLatitude()).isEqualTo(37.50);
        assertThat(savedPlan.getTotalStops()).isEqualTo(1);
        assertThat(savedStop.getAddress()).isEqualTo("서울시청");
        assertThat(savedStop.getDeliveryItems()).hasSize(1);
        assertThat(savedStop.getRiskAssessment().getLevel())
                .isEqualTo(RiskLevel.UNKNOWN);

        ArgumentCaptor<DeliveryPlanCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryPlanCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().planId()).isEqualTo(100L);
    }

    @Test
    void 존재하지_않는_기사로는_배송계획을_생성할_수_없다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        CreateDeliveryPlanRequest request =
                new CreateDeliveryPlanRequest(
                        "서울 물류센터",
                        LocalDateTime.now().plusHours(1),
                        List.of()
                );

        assertThatThrownBy(() -> facade.create(999L, request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ExceptionCode.INVALID_INPUT)
                );

        verify(geocodingClient, never()).geocode(any());
        verify(deliveryPlanRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
