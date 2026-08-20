package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.dto.response.NextStopRecommendationResponse;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.service.component.DrivingDirectionsClient;
import com.example.delivery_project.service.component.route.DijkstraRouteOptimizer;
import com.example.delivery_project.spec.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NextStopRecommendationServiceTest {

    @Mock
    private DeliveryPlanRepository deliveryPlanRepository;

    @Mock
    private DrivingDirectionsClient drivingDirectionsClient;

    private NextStopRecommendationService service;
    private DeliveryPlan plan;

    @BeforeEach
    void setUp() {
        service = new NextStopRecommendationService(
                deliveryPlanRepository,
                new DijkstraRouteOptimizer(),
                drivingDirectionsClient
        );
        lenient().when(drivingDirectionsClient.findTravelDurationSeconds(
                        anyDouble(),
                        anyDouble(),
                        anyDouble(),
                        anyDouble()
                ))
                .thenReturn(OptionalLong.empty());

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
    }

    @Test
    void 완료한_배송지_다음_최대_5곳에서_위험도가_가장_낮은_곳을_추천한다() {
        DeliveryStop first = addStop(101L, 37.501, 126.901);
        DeliveryStop second = addStop(102L, 37.502, 126.902);
        DeliveryStop third = addStop(103L, 37.503, 126.903);
        DeliveryStop fourth = addStop(104L, 37.504, 126.904);
        DeliveryStop fifth = addStop(105L, 37.505, 126.905);
        DeliveryStop sixth = addStop(106L, 37.506, 126.906);
        DeliveryStop seventh = addStop(107L, 37.5001, 126.9001);

        markKnown(second, List.of(RiskFactorType.HEAVY_RAIN));
        markKnown(third, List.of(RiskFactorType.HEAT_WAVE));
        markKnown(fourth, List.of());
        markKnown(fifth, List.of(RiskFactorType.WEATHER_WARNING));
        markKnown(sixth, List.of(
                RiskFactorType.HEAVY_RAIN,
                RiskFactorType.HEAT_WAVE
        ));
        markKnown(seventh, List.of());

        plan.start();
        plan.completeStop(first.getId());
        when(deliveryPlanRepository.findByIdAndDriverId(10L, 1L))
                .thenReturn(Optional.of(plan));
        when(drivingDirectionsClient.findTravelDurationSeconds(
                        37.501,
                        126.901,
                        37.504,
                        126.904
                ))
                .thenReturn(OptionalLong.of(1_125));

        NextStopRecommendationResponse response = service.recommend(10L, 1L);

        assertThat(response.available()).isTrue();
        assertThat(response.currentStopId()).isEqualTo(101L);
        assertThat(response.candidateStopIds())
                .containsExactly(102L, 103L, 104L, 105L, 106L);
        assertThat(response.candidateCount()).isEqualTo(5);
        assertThat(response.recommendedStopId()).isEqualTo(104L);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.SAFE);
        assertThat(response.riskScore()).isZero();
        assertThat(response.optimizedSafestRouteStopIds())
                .containsExactly(104L);
        assertThat(response.estimatedTravelSeconds()).isPositive();
        assertThat(response.kakaoTravelSeconds()).isEqualTo(1_125);
    }

    @Test
    void 위험도가_같으면_다익스트라_경로의_첫_배송지를_추천한다() {
        DeliveryStop first = addStop(101L, 37.50, 126.90);
        DeliveryStop farStop = addStop(102L, 37.60, 127.00);
        DeliveryStop nearStop = addStop(103L, 37.51, 126.91);
        markKnown(farStop, List.of());
        markKnown(nearStop, List.of());

        plan.start();
        plan.completeStop(first.getId());
        when(deliveryPlanRepository.findByIdAndDriverId(10L, 1L))
                .thenReturn(Optional.of(plan));

        NextStopRecommendationResponse response = service.recommend(10L, 1L);

        assertThat(response.recommendedStopId()).isEqualTo(103L);
        assertThat(response.optimizedSafestRouteStopIds())
                .containsExactly(103L, 102L);
        assertThat(response.estimatedTravelSeconds()).isPositive();
        assertThat(response.kakaoTravelSeconds()).isNull();
    }

    @Test
    void 위험도_미확인은_점수_마이너스여도_안전한_배송지보다_우선하지_않는다() {
        DeliveryStop first = addStop(101L, 37.50, 126.90);
        DeliveryStop unknownStop = addStop(102L, 37.5001, 126.9001);
        DeliveryStop safeStop = addStop(103L, 37.52, 126.92);
        markKnown(safeStop, List.of());

        plan.start();
        plan.completeStop(first.getId());
        when(deliveryPlanRepository.findByIdAndDriverId(10L, 1L))
                .thenReturn(Optional.of(plan));

        NextStopRecommendationResponse response = service.recommend(10L, 1L);

        assertThat(unknownStop.getRiskAssessment().getScore()).isEqualTo(-1);
        assertThat(response.recommendedStopId()).isEqualTo(103L);
    }

    @Test
    void 배송중이_아니면_추천할_수_없다() {
        addStop(101L, 37.50, 126.90);
        when(deliveryPlanRepository.findByIdAndDriverId(10L, 1L))
                .thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.recommend(10L, 1L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        DeliveryException.DELIVERY_RECOMMENDATION_NOT_AVAILABLE
                                )
                );
    }

    private DeliveryStop addStop(
            Long id,
            double latitude,
            double longitude
    ) {
        DeliveryStop stop = plan.addStop(
                "배송지 " + id,
                latitude,
                longitude,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(stop, "id", id);
        return stop;
    }

    private void markKnown(
            DeliveryStop stop,
            List<RiskFactorType> factors
    ) {
        stop.getRiskAssessment().replaceFactors(
                factors,
                LocalDateTime.now()
        );
    }
}
