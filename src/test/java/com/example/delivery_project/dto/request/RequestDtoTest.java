package com.example.delivery_project.dto.request;

import com.example.delivery_project.enums.ProductType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestDtoTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void 배송계획과_배송지의_null_목록을_빈_불변목록으로_바꾼다() {
        CreateDeliveryStopRequest stop =
                new CreateDeliveryStopRequest("배송지", null);
        CreateDeliveryPlanRequest plan =
                new CreateDeliveryPlanRequest(
                        "물류센터",
                        LocalDateTime.now(),
                        null
                );

        assertThat(stop.items()).isEmpty();
        assertThat(plan.stops()).isEmpty();
        assertThatThrownBy(() -> stop.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> plan.stops().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 전달받은_목록을_복사해_외부_변경으로부터_보호한다() {
        List<CreateDeliveryStopRequest> stops = new ArrayList<>();
        stops.add(new CreateDeliveryStopRequest("배송지", List.of()));
        CreateDeliveryPlanRequest request =
                new CreateDeliveryPlanRequest(
                        "물류센터",
                        LocalDateTime.now(),
                        stops
                );

        stops.clear();

        assertThat(request.stops()).hasSize(1);
    }

    @Test
    void 배송계획_생성요청의_중첩된_필수값을_검증한다() {
        CreateDeliveryPlanRequest request = new CreateDeliveryPlanRequest(
                " ",
                null,
                List.of(new CreateDeliveryStopRequest(
                        " ",
                        List.of(new CreateDeliveryItemRequest(
                                " ",
                                null,
                                0
                        ))
                ))
        );

        Set<ConstraintViolation<CreateDeliveryPlanRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                        "departureAddress",
                        "scheduledDepartureAt",
                        "stops[0].address",
                        "stops[0].items[0].productName",
                        "stops[0].items[0].productType",
                        "stops[0].items[0].quantity"
                );
    }

    @Test
    void 정상적인_배송계획_생성요청은_검증을_통과한다() {
        CreateDeliveryPlanRequest request = new CreateDeliveryPlanRequest(
                "서울 물류센터",
                LocalDateTime.now().plusHours(1),
                List.of(new CreateDeliveryStopRequest(
                        "서울시청",
                        List.of(new CreateDeliveryItemRequest(
                                "냉동식품",
                                ProductType.FROZEN,
                                1
                        ))
                ))
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
