package com.example.delivery_project.dto.response;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.enums.RiskLevel;

import java.util.List;

public record NextStopRecommendationResponse(
        boolean available,
        Long currentStopId,
        Long recommendedStopId,
        String address,
        Double latitude,
        Double longitude,
        RiskLevel riskLevel,
        Integer riskScore,
        int candidateCount,
        List<Long> candidateStopIds,
        List<Long> optimizedSafestRouteStopIds,
        Long estimatedTravelSeconds
) {
    public NextStopRecommendationResponse {
        candidateStopIds = List.copyOf(candidateStopIds);
        optimizedSafestRouteStopIds = List.copyOf(optimizedSafestRouteStopIds);
    }

    public static NextStopRecommendationResponse unavailable(
            Long currentStopId
    ) {
        return new NextStopRecommendationResponse(
                false,
                currentStopId,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                List.of(),
                List.of(),
                null
        );
    }

    public static NextStopRecommendationResponse available(
            Long currentStopId,
            DeliveryStop recommendedStop,
            int candidateCount,
            List<Long> candidateStopIds,
            List<Long> optimizedSafestRouteStopIds,
            long estimatedTravelSeconds
    ) {
        return new NextStopRecommendationResponse(
                true,
                currentStopId,
                recommendedStop.getId(),
                recommendedStop.getAddress(),
                recommendedStop.getLatitude(),
                recommendedStop.getLongitude(),
                recommendedStop.getRiskAssessment().getLevel(),
                recommendedStop.getRiskAssessment().getScore(),
                candidateCount,
                candidateStopIds,
                optimizedSafestRouteStopIds,
                estimatedTravelSeconds
        );
    }
}
