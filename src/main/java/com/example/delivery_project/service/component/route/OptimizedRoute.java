package com.example.delivery_project.service.component.route;

import java.util.List;

public record OptimizedRoute(
        List<Long> stopIds,
        long totalDurationSeconds,
        int expandedStateCount
) {

    public OptimizedRoute {
        stopIds = List.copyOf(stopIds);
    }
}
