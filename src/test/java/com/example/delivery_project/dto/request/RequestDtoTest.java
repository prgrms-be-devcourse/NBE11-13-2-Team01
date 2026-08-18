package com.example.delivery_project.dto.request;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestDtoTest {

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
}
