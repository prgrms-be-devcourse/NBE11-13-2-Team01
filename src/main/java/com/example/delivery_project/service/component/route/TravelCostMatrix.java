package com.example.delivery_project.service.component.route;

import java.util.Map;
import java.util.OptionalLong;

public record TravelCostMatrix(
        Map<RouteLeg, Long> travelDurationSeconds
) {

    public TravelCostMatrix {
        travelDurationSeconds = Map.copyOf(
                travelDurationSeconds
        );

        if (travelDurationSeconds.values().stream()
                .anyMatch(duration -> duration < 0)) {
            throw new IllegalArgumentException(
                    "이동시간은 음수일 수 없습니다."
            );
        }
    }

    public OptionalLong findDuration(
            Long fromStopId,
            Long toStopId
    ) {
        Long duration = travelDurationSeconds.get(new RouteLeg(fromStopId, toStopId));

        if (duration == null) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(duration);
    }
}
