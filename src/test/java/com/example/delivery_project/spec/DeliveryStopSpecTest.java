package com.example.delivery_project.spec;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryStopSpecTest {

    @Test
    void nullable_child_lists_are_normalized_to_empty_lists() {
        DeliveryStopSpec spec = new DeliveryStopSpec(
                new Location("목적지", 37.49, 127.03),
                null,
                null
        );

        assertThat(spec.items()).isEmpty();
        assertThat(spec.riskFactors()).isEmpty();
    }

    @Test
    void child_lists_are_defensively_copied() {
        List<DeliveryItemSpec> items = new ArrayList<>();
        DeliveryStopSpec spec = new DeliveryStopSpec(
                new Location("목적지", 37.49, 127.03),
                items,
                List.of()
        );

        items.add(null);

        assertThat(spec.items()).isEmpty();
        assertThatThrownBy(() -> spec.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
