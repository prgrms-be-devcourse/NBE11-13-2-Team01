package com.example.delivery_project.service.component.route;

import java.util.HashSet;
import java.util.List;

public record RouteOptimizationContext(
        Long currentStopId,
        List<Long> candidateStopIds,
        TravelCostMatrix travelCostMatrix
) {

    public RouteOptimizationContext {
        candidateStopIds = List.copyOf(candidateStopIds);

        if (new HashSet<>(candidateStopIds).size()
                != candidateStopIds.size()) {
            throw new IllegalArgumentException(
                    "후보 배송지 ID는 중복될 수 없습니다."
            );
        }

        if (candidateStopIds.contains(currentStopId)) {
            throw new IllegalArgumentException(
                    "현재 배송지는 후보 배송지에 포함될 수 없습니다."
            );
        }
    }
}
