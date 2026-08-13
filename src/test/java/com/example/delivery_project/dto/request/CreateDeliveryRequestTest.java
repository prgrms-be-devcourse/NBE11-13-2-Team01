package com.example.delivery_project.dto.request;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateDeliveryRequestTest {

    @Test
    void plan_request_normalizes_and_copies_stop_list() {
        CreateDeliveryPlanRequest emptyRequest = new CreateDeliveryPlanRequest(
                "출발지",
                37.5,
                127.0,
                LocalDateTime.of(2026, 8, 14, 9, 0),
                null
        );

        assertThat(emptyRequest.stops()).isEmpty();
        assertThat(emptyRequest.toDepartureLocation().address()).isEqualTo("출발지");

        List<CreateDeliveryStopRequest> stops = new ArrayList<>();
        CreateDeliveryPlanRequest copiedRequest = new CreateDeliveryPlanRequest(
                "출발지",
                37.5,
                127.0,
                LocalDateTime.of(2026, 8, 14, 9, 0),
                stops
        );
        stops.add(new CreateDeliveryStopRequest("목적지", 37.49, 127.03, null));

        assertThat(copiedRequest.stops()).isEmpty();
        assertThatThrownBy(() -> copiedRequest.stops().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void stop_request_normalizes_and_copies_item_list() {
        CreateDeliveryStopRequest emptyRequest =
                new CreateDeliveryStopRequest("목적지", 37.49, 127.03, null);

        assertThat(emptyRequest.items()).isEmpty();

        List<CreateDeliveryItemRequest> items = new ArrayList<>();
        CreateDeliveryStopRequest copiedRequest =
                new CreateDeliveryStopRequest("목적지", 37.49, 127.03, items);
        items.add(null);

        assertThat(copiedRequest.items()).isEmpty();
        assertThatThrownBy(() -> copiedRequest.items().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
