package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.dto.projection.DeliveryPlanSummaryProjection;
import com.example.delivery_project.dto.response.DeliveryPlanDetailResponse;
import com.example.delivery_project.enums.DeliveryStopStatus;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.spec.Location;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@ActiveProfiles("db-test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@EnabledIfEnvironmentVariable(
        named = "TEST_DB_URL",
        matches = ".+"
)
class DeliveryRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliveryPlanRepository deliveryPlanRepository;

    @Autowired
    private DeliveryStopRepository deliveryStopRepository;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void 배송계획_연관관계와_활성_배송지_쿼리를_검증한다() {
        User driver = userRepository.save(User.of(
                "repository-driver",
                "password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        ));
        DeliveryPlan plan = DeliveryPlanFactory.create(
                driver,
                new Location("서울 물류센터", 37.50, 126.90),
                LocalDateTime.now().plusHours(1)
        );
        DeliveryStop first = plan.addStop(
                "배송지 1",
                37.51,
                126.91,
                LocalDateTime.now()
        );
        plan.addStop(
                "배송지 2",
                37.52,
                126.92,
                LocalDateTime.now()
        );
        first.addItem("상품", ProductType.NORMAL, 1);
        DeliveryPlan savedPlan =
                deliveryPlanRepository.saveAndFlush(plan);

        assertThat(deliveryPlanRepository.findByIdAndDriverId(
                savedPlan.getId(),
                driver.getId()
        )).isPresent();
        assertThat(deliveryPlanRepository.findByIdAndDriverId(
                savedPlan.getId(),
                Long.MAX_VALUE
        )).isEmpty();

        DeliveryPlanSummaryProjection summary = deliveryPlanRepository
                .findAllSummariesByDriverId(driver.getId())
                .getFirst();
        assertThat(summary.getPlanId()).isEqualTo(savedPlan.getId());
        assertThat(summary.getDriverId()).isEqualTo(driver.getId());
        assertThat(summary.getTotalStops().longValue()).isEqualTo(2L);
        assertThat(summary.getRemainingStops().longValue()).isEqualTo(2L);
        assertThat(summary.getTotalBoxes().longValue()).isEqualTo(1L);
        assertThat(summary.getRemainingBoxes().longValue()).isEqualTo(1L);
        assertThat(summary.getDangerStops().longValue()).isZero();
        assertThat(deliveryPlanRepository.findAllSummaries())
                .extracting(DeliveryPlanSummaryProjection::getPlanId)
                .contains(savedPlan.getId());

        List<DeliveryStop> readyStops =
                deliveryStopRepository.findAllWithRiskByStatusIn(
                        List.of(DeliveryStopStatus.READY)
        );
        List<DeliveryStop> planStops = deliveryStopRepository
                .findAllWithRiskByDeliveryPlanIdAndStatusIn(
                        savedPlan.getId(),
                        List.of(DeliveryStopStatus.READY)
                );

        assertThat(readyStops).hasSize(2);
        assertThat(planStops).hasSize(2);
        DeliveryStop detail = deliveryStopRepository
                .findDetailByIdAndPlanId(
                first.getId(),
                savedPlan.getId()
        ).orElseThrow();
        assertThat(detail.getDeliveryItems()).hasSize(1);

        savedPlan.start();
        deliveryPlanRepository.flush();

        assertThat(deliveryStopRepository.findAllWithRiskByStatusIn(
                List.of(DeliveryStopStatus.DELIVERING)
        )).hasSize(2);
    }

    @Test
    void 날씨_시간범위_조회와_UPSERT용_UPDATE_쿼리를_검증한다() {
        LocalDate date = LocalDate.of(2026, 8, 18);
        LocalTime forecastTime = LocalTime.of(11, 0);
        weatherRepository.saveAndFlush(Weather.of(
                60,
                127,
                date,
                forecastTime,
                date,
                LocalTime.of(10, 30),
                "T1H",
                "32"
        ));

        assertThat(weatherRepository
                .findByNxAndNyAndFcstDateBetweenAndCategoryIn(
                        60,
                        127,
                        date.minusDays(1),
                        date,
                        List.of("T1H", "RN1", "PTY")
                )).hasSize(1);

        int updated = weatherRepository.updateFcstValue(
                60,
                127,
                date,
                forecastTime,
                date,
                LocalTime.of(10, 30),
                "T1H",
                "33",
                LocalDateTime.now()
        );
        weatherRepository.flush();
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(weatherRepository
                .findByNxAndNyAndFcstDateAndFcstTimeAndCategoryIn(
                        60,
                        127,
                        date,
                        forecastTime,
                        List.of("T1H")
                )).singleElement()
                .extracting(Weather::getFcstValue)
                .isEqualTo("33");
    }

    @Test
    void 배송지_수가_늘어도_상세_조회는_세_쿼리로_고정된다() {
        User driver = userRepository.save(User.of(
                "detail-query-driver",
                "password",
                "상세조회기사",
                Role.ROLE_DELIVERY_DRIVER
        ));
        DeliveryPlan plan = DeliveryPlanFactory.create(
                driver,
                new Location("서울 물류센터", 37.50, 126.90),
                LocalDateTime.now().plusHours(1)
        );
        for (int index = 0; index < 10; index++) {
            DeliveryStop stop = plan.addStop(
                    "배송지 " + index,
                    37.51 + index * 0.001,
                    126.91 + index * 0.001,
                    LocalDateTime.now()
            );
            stop.addItem("상품 " + index, ProductType.NORMAL, index + 1);
            stop.getRiskAssessment().replaceFactors(
                    List.of(RiskFactorType.HEAVY_RAIN),
                    LocalDateTime.now()
            );
        }
        DeliveryPlan savedPlan = deliveryPlanRepository.saveAndFlush(plan);
        entityManager.clear();

        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        DeliveryPlan loadedPlan = deliveryPlanRepository
                .findDetailById(savedPlan.getId())
                .orElseThrow();
        deliveryStopRepository.findAllWithItemsByDeliveryPlanId(
                savedPlan.getId()
        );
        riskAssessmentRepository.findAllWithFactorsByDeliveryPlanId(
                savedPlan.getId()
        );
        DeliveryPlanDetailResponse response =
                DeliveryPlanDetailResponse.from(loadedPlan);

        assertThat(response.deliveryStops()).hasSize(10);
        assertThat(response.deliveryStops())
                .allSatisfy(stop -> {
                    assertThat(stop.deliveryItems()).hasSize(1);
                    assertThat(stop.riskAssessment().factors()).hasSize(1);
                });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3L);
    }
}
