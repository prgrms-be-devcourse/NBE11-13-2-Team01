package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.spec.Location;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryItemValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void blank_name_and_non_positive_quantity_are_invalid() {
        DeliveryPlan plan = DeliveryPlanFactory.create(
                User.of("driver", "password", "배송 기사"),
                new Location("출발지", 37.5, 127.0),
                LocalDateTime.of(2026, 8, 14, 9, 0)
        );
        DeliveryStop stop = plan.addStop("목적지", 37.49, 127.03, LocalDateTime.now());
        DeliveryItem item = stop.addItem(" ", ProductType.NORMAL, 0);

        assertThat(validator.validate(item))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("productName", "quantity");
    }
}
