package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.RiskAssessmentRepository;
import com.example.delivery_project.dto.response.NextStopRecommendationResponse;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.service.component.DrivingDirectionsClient;
import com.example.delivery_project.service.component.route.OptimizedRoute;
import com.example.delivery_project.service.component.route.RouteLeg;
import com.example.delivery_project.service.component.route.RouteOptimizationContext;
import com.example.delivery_project.service.component.route.RouteOptimizer;
import com.example.delivery_project.service.component.route.TravelCostMatrix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NextStopRecommendationService {

    private static final int CANDIDATE_LIMIT = 5;
    private static final long DEPARTURE_NODE_ID = Long.MIN_VALUE;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final double ESTIMATED_SPEED_METERS_PER_SECOND = 30_000.0 / 3_600.0;

    private final DeliveryPlanRepository deliveryPlanRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RouteOptimizer routeOptimizer;
    private final DrivingDirectionsClient drivingDirectionsClient;

    public NextStopRecommendationResponse recommend(
            Long planId,
            Long driverId
    ) {
        DeliveryPlan plan = getOwnedPlan(planId, driverId);
        validateDelivering(plan);

        RoutePoint currentPoint = resolveCurrentPoint(plan);
        List<DeliveryStop> candidates = findCandidates(plan);

        if (candidates.isEmpty()) {
            return NextStopRecommendationResponse.unavailable(
                    currentPoint.responseStopId()
            );
        }

        riskAssessmentRepository.findAllWithFactorsByDeliveryStopIdIn(
                candidates.stream()
                        .map(DeliveryStop::getId)
                        .toList()
        );

        RiskPriority safestPriority = candidates.stream()
                .map(this::riskPriority)
                .min(RiskPriority.COMPARATOR)
                .orElseThrow();

        List<DeliveryStop> safestCandidates = candidates.stream()
                .filter(stop -> riskPriority(stop).equals(safestPriority))
                .toList();

        TravelCostMatrix travelCostMatrix = createTravelCostMatrix(
                currentPoint,
                safestCandidates
        );
        OptimizedRoute optimizedRoute = routeOptimizer.optimize(
                new RouteOptimizationContext(
                        currentPoint.nodeId(),
                        safestCandidates.stream()
                                .map(DeliveryStop::getId)
                                .toList(),
                        travelCostMatrix
                )
        );

        Long recommendedStopId = optimizedRoute.stopIds().getFirst();
        DeliveryStop recommendedStop = safestCandidates.stream()
                .filter(stop -> stop.getId().equals(recommendedStopId))
                .findFirst()
                .orElseThrow();
        long estimatedTravelSeconds = travelCostMatrix.findDuration(
                        currentPoint.nodeId(),
                        recommendedStopId
                )
                .orElseThrow();
        OptionalLong kakaoTravelDuration =
                drivingDirectionsClient.findTravelDurationSeconds(
                        currentPoint.latitude(),
                        currentPoint.longitude(),
                        recommendedStop.getLatitude(),
                        recommendedStop.getLongitude()
                );
        Long kakaoTravelSeconds = kakaoTravelDuration.isPresent()
                ? kakaoTravelDuration.getAsLong()
                : null;

        log.info(
                "[ROUTE] 다음 배송지 추천 planId: {}, currentStopId: {}, "
                        + "candidateStopIds: {}, recommendedStopId: {}",
                planId,
                currentPoint.responseStopId(),
                candidates.stream().map(DeliveryStop::getId).toList(),
                recommendedStopId
        );

        return NextStopRecommendationResponse.available(
                currentPoint.responseStopId(),
                recommendedStop,
                candidates.size(),
                candidates.stream().map(DeliveryStop::getId).toList(),
                optimizedRoute.stopIds(),
                estimatedTravelSeconds,
                kakaoTravelSeconds
        );
    }

    private DeliveryPlan getOwnedPlan(
            Long planId,
            Long driverId
    ) {
        return deliveryPlanRepository
                .findWithStopsAndRiskByIdAndDriverId(planId, driverId)
                .orElseThrow(() -> new BusinessException(
                        DeliveryException.DELIVERY_PLAN_NOT_FOUND
                ));
    }

    private void validateDelivering(DeliveryPlan plan) {
        if (!plan.getStatus().isDelivering()) {
            throw new BusinessException(
                    DeliveryException.DELIVERY_RECOMMENDATION_NOT_AVAILABLE
            );
        }
    }

    private List<DeliveryStop> findCandidates(DeliveryPlan plan) {
        return plan.getDeliveryStops().stream()
                .filter(stop -> !stop.getStatus().isCompleted())
                .limit(CANDIDATE_LIMIT)
                .toList();
    }

    private RoutePoint resolveCurrentPoint(DeliveryPlan plan) {
        return plan.getDeliveryStops().stream()
                .filter(stop -> stop.getStatus().isCompleted())
                .filter(stop -> stop.getCompletedAt() != null)
                .max(Comparator.comparing(DeliveryStop::getCompletedAt))
                .map(stop -> new RoutePoint(
                        stop.getId(),
                        stop.getId(),
                        stop.getLatitude(),
                        stop.getLongitude()
                ))
                .orElseGet(() -> new RoutePoint(
                        DEPARTURE_NODE_ID,
                        null,
                        plan.getDepartureLatitude(),
                        plan.getDepartureLongitude()
                ));
    }

    private RiskPriority riskPriority(DeliveryStop stop) {
        RiskAssessment assessment = stop.getRiskAssessment();

        if (assessment == null
                || assessment.getLevel() == RiskLevel.UNKNOWN
                || assessment.getScore() < 0) {
            return new RiskPriority(3, Integer.MAX_VALUE);
        }

        int levelOrder = switch (assessment.getLevel()) {
            case SAFE -> 0;
            case CAUTION -> 1;
            case DANGER -> 2;
            case UNKNOWN -> 3;
        };

        return new RiskPriority(levelOrder, assessment.getScore());
    }

    private TravelCostMatrix createTravelCostMatrix(
            RoutePoint currentPoint,
            List<DeliveryStop> candidates
    ) {
        List<RoutePoint> points = new ArrayList<>();
        points.add(currentPoint);
        points.addAll(candidates.stream()
                .map(stop -> new RoutePoint(
                        stop.getId(),
                        stop.getId(),
                        stop.getLatitude(),
                        stop.getLongitude()
                ))
                .toList());

        Map<RouteLeg, Long> durations = new HashMap<>();

        for (RoutePoint from : points) {
            for (RoutePoint to : points) {
                if (from.nodeId().equals(to.nodeId())
                        || to.nodeId().equals(currentPoint.nodeId())) {
                    continue;
                }

                durations.put(
                        new RouteLeg(from.nodeId(), to.nodeId()),
                        estimateTravelSeconds(from, to)
                );
            }
        }

        return new TravelCostMatrix(durations);
    }

    private long estimateTravelSeconds(
            RoutePoint from,
            RoutePoint to
    ) {
        double latitudeDistance = Math.toRadians(
                to.latitude() - from.latitude()
        );
        double longitudeDistance = Math.toRadians(
                to.longitude() - from.longitude()
        );
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());

        double haversine = Math.pow(Math.sin(latitudeDistance / 2), 2)
                + Math.cos(fromLatitude)
                * Math.cos(toLatitude)
                * Math.pow(Math.sin(longitudeDistance / 2), 2);
        double distanceMeters = 2
                * EARTH_RADIUS_METERS
                * Math.asin(Math.sqrt(haversine));

        return Math.max(
                1L,
                (long) Math.ceil(
                        distanceMeters / ESTIMATED_SPEED_METERS_PER_SECOND
                )
        );
    }

    private record RoutePoint(
            Long nodeId,
            Long responseStopId,
            Double latitude,
            Double longitude
    ) {
    }

    private record RiskPriority(
            int levelOrder,
            int score
    ) {
        private static final Comparator<RiskPriority> COMPARATOR =
                Comparator.comparingInt(RiskPriority::levelOrder)
                        .thenComparingInt(RiskPriority::score);
    }
}
