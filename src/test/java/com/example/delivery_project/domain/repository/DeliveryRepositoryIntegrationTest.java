package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.delivery.DeliveryItem;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.spec.DeliveryItemSpec;
import com.example.delivery_project.spec.DeliveryStopSpec;
import com.example.delivery_project.spec.Location;
import com.example.delivery_project.spec.RiskFactorSpec;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("db")
@DataJpaTest
@ActiveProfiles("db-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeliveryRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliveryPlanRepository deliveryPlanRepository;

    @Autowired
    private DeliveryStopRepository deliveryStopRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saving_plan_cascades_the_entire_aggregate() {
        User driver = saveDriver("driver-aggregate");
        DeliveryPlan plan = createPlanWithStopItemAndFactors(driver);

        DeliveryPlan savedPlan = deliveryPlanRepository.saveAndFlush(plan);
        Long planId = savedPlan.getId();
        entityManager.clear();

        DeliveryPlan foundPlan = deliveryPlanRepository.findById(planId).orElseThrow();
        DeliveryStop foundStop = foundPlan.getDeliveryStops().getFirst();
        DeliveryItem foundItem = foundStop.getDeliveryItems().getFirst();
        RiskAssessment foundAssessment = foundStop.getRiskAssessment();

        assertThat(foundPlan.getId()).isNotNull();
        assertThat(foundStop.getId()).isNotNull();
        assertThat(foundStop.getDeliveryPlan().getId()).isEqualTo(planId);

        assertThat(foundItem.getId()).isNotNull();
        assertThat(foundItem.getDeliveryStop().getId()).isEqualTo(foundStop.getId());
        assertThat(foundItem.getProductName()).isEqualTo("생수");

        assertThat(foundAssessment.getId()).isNotNull();
        assertThat(foundAssessment.getDeliveryStop().getId()).isEqualTo(foundStop.getId());
        assertThat(foundAssessment.getLevel()).isEqualTo(RiskLevel.DANGER);
        assertThat(foundAssessment.getRiskFactors()).hasSize(2)
                .allSatisfy(factor -> {
                    assertThat(factor.getId()).isNotNull();
                    assertThat(factor.getRiskAssessment().getId())
                            .isEqualTo(foundAssessment.getId());
                });
    }

    @Test
    void detail_query_returns_a_plan_without_stops() {
        User driver = saveDriver("driver-empty-plan");
        DeliveryPlan emptyPlan = DeliveryPlanFactory.create(
                driver,
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
        Long planId = deliveryPlanRepository.saveAndFlush(emptyPlan).getId();
        entityManager.clear();

        var foundPlan = deliveryPlanRepository.findDetailById(planId);

        assertThat(foundPlan).isPresent();
        assertThat(foundPlan.orElseThrow().getDeliveryStops()).isEmpty();
    }

    @Test
    void detail_query_returns_a_stop_without_items_and_checks_plan_scope() {
        User driver = saveDriver("driver-empty-stop");
        DeliveryPlan firstPlan = DeliveryPlanFactory.create(
                driver,
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
        DeliveryStop emptyStop = firstPlan.addStop(
                "빈 목적지",
                37.49,
                127.03,
                LocalDateTime.now()
        );
        DeliveryPlan secondPlan = DeliveryPlanFactory.create(
                driver,
                new Location("다른 출발지", 37.4, 127.1),
                LocalDateTime.of(2026, 8, 14, 10, 0)
        );
        deliveryPlanRepository.save(firstPlan);
        deliveryPlanRepository.save(secondPlan);
        deliveryPlanRepository.flush();
        Long firstPlanId = firstPlan.getId();
        Long secondPlanId = secondPlan.getId();
        Long stopId = emptyStop.getId();
        entityManager.clear();

        var foundStop = deliveryStopRepository.findDetailByIdAndPlanId(
                stopId,
                firstPlanId
        );
        var stopFromWrongPlan = deliveryStopRepository.findDetailByIdAndPlanId(
                stopId,
                secondPlanId
        );

        assertThat(foundStop).isPresent();
        assertThat(foundStop.orElseThrow().getDeliveryItems()).isEmpty();
        assertThat(stopFromWrongPlan).isEmpty();
    }

    @Test
    void reordered_stop_sequence_is_persisted() {
        User driver = saveDriver("driver-order");
        DeliveryPlan plan = DeliveryPlanFactory.create(
                driver,
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
        DeliveryStop first = plan.addStop("첫 번째", 37.49, 127.03, LocalDateTime.now());
        DeliveryStop second = plan.addStop("두 번째", 37.48, 127.04, LocalDateTime.now());
        deliveryPlanRepository.saveAndFlush(plan);

        plan.reorderStops(List.of(second.getId(), first.getId()));
        deliveryPlanRepository.flush();
        Long planId = plan.getId();
        entityManager.clear();

        DeliveryPlan reloadedPlan = deliveryPlanRepository.findById(planId).orElseThrow();

        assertThat(reloadedPlan.getDeliveryStops())
                .extracting(DeliveryStop::getAddress)
                .containsExactly("두 번째", "첫 번째");
    }

    @Test
    void plans_are_filtered_by_driver_id() {
        User firstDriver = saveDriver("driver-first");
        User secondDriver = saveDriver("driver-second");
        DeliveryPlan firstPlan = createEmptyPlan(firstDriver, "첫 번째 출발지");
        DeliveryPlan secondPlan = createEmptyPlan(secondDriver, "두 번째 출발지");
        deliveryPlanRepository.saveAllAndFlush(List.of(firstPlan, secondPlan));
        entityManager.clear();

        List<DeliveryPlan> firstDriverPlans =
                deliveryPlanRepository.findAllByDriverId(firstDriver.getId());

        assertThat(firstDriverPlans).singleElement()
                .extracting(DeliveryPlan::getDepartureLocation)
                .isEqualTo("첫 번째 출발지");
    }

    private User saveDriver(String loginId) {
        return userRepository.saveAndFlush(
                User.of(loginId, "password", "배송 기사")
        );
    }

    private DeliveryPlan createPlanWithStopItemAndFactors(User driver) {
        DeliveryStopSpec stopSpec = new DeliveryStopSpec(
                new Location("목적지", 37.49, 127.03),
                List.of(new DeliveryItemSpec("생수", ProductType.NORMAL, 2)),
                List.of(
                        new RiskFactorSpec(RiskFactorType.HEAVY_RAIN, "폭우"),
                        new RiskFactorSpec(RiskFactorType.WEATHER_WARNING, "호우 특보")
                )
        );
        return DeliveryPlanFactory.create(
                driver,
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0),
                List.of(stopSpec)
        );
    }

    private DeliveryPlan createEmptyPlan(User driver, String departureAddress) {
        return DeliveryPlanFactory.create(
                driver,
                new Location(departureAddress, 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
    }
}
