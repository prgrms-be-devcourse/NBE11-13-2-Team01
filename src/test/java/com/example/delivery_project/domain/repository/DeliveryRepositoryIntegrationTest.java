package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.enums.DeliveryStopStatus;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.spec.Location;
import jakarta.persistence.EntityManager;
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

@DataJpaTest(properties = "spring.sql.init.mode=never")
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
    private WeatherRepository weatherRepository;

    @Autowired
    private EntityManager entityManager;

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

        List<DeliveryStop> readyStops =
                deliveryStopRepository.findAllByStatusIn(
                        List.of(DeliveryStopStatus.READY)
                );
        List<DeliveryStop> planStops = deliveryStopRepository
                .findAllByDeliveryPlanIdAndStatusIn(
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

        assertThat(deliveryStopRepository.findAllByStatusIn(
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
}
