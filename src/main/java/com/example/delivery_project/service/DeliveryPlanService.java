package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.domain.repository.GridCoordinate;
import com.example.delivery_project.dto.request.UpdateDeliveryOrderRequest;
import com.example.delivery_project.dto.request.UpdateScheduledDepartureRequest;
import com.example.delivery_project.dto.response.DeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.DeliveryPlanSummaryResponse;
import com.example.delivery_project.dto.response.DeliveryStopResponse;
import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.service.component.LocationConverter;
import com.example.delivery_project.service.component.WeatherUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DeliveryPlanService {
    private final DeliveryPlanRepository deliveryPlanRepository;
    private final DeliveryStopRepository deliveryStopRepository;
    private final WeatherUpdater weatherUpdater;

    private static final String PLAN = "[PLAN]";
    private static final String STOP = "[STOP]";

    public List<DeliveryPlanSummaryResponse> getDeliveryPlans(Long driverId) {
        List<DeliveryPlan> deliveryPlans = deliveryPlanRepository.findAllByDriverId(driverId);

        log.info("{} 목록 조회 완료 driverId: {}, planSize: {} ", PLAN, driverId, deliveryPlans.size());
        return deliveryPlans.stream()
                .map(DeliveryPlanSummaryResponse::from)
                .toList();
    }

    public DeliveryPlanDetailResponse getDeliveryPlan(Long planId) {
        DeliveryPlan plan = getPlanIfExists(planId);

        log.info("{} 조회 완료 planId: {}", PLAN, planId);
        return DeliveryPlanDetailResponse.from(plan);
    }

    public DeliveryStopResponse getDeliveryStop(
            Long planId,
            Long stopId
    ) {
        DeliveryStop stop = deliveryStopRepository.findDetailByIdAndPlanId(stopId, planId)
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_STOP_NOT_FOUND));
        log.info("{} 조회 완료 planId: {}, stopId: {}", STOP, planId, stopId);

        return DeliveryStopResponse.from(stop);
    }

    @Transactional
    public void changeScheduledDepartureAt(
            Long planId,
            UpdateScheduledDepartureRequest request
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 예정 시간 변경 요청 planId: {}, 변경 요청 시간: {}", PLAN, planId, request.scheduledDepartureAt());
        plan.updateScheduledDepartureAt(request.scheduledDepartureAt());
    }

    @Transactional
    public void reorderStops(
            Long planId,
            UpdateDeliveryOrderRequest request
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 순서 편집 요청 planId: {}", PLAN, planId);
        plan.reorderStops(request.stopIds());
    }

    @Transactional
    public void start(
            Long planId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 배송 시작 요청 planId: {}", PLAN, planId);
        // TODO 커스텀 예외로 변경
        DeliveryPlan plan = deliveryPlanRepository.findById(planId)
                .orElseThrow(IllegalStateException::new);

        upsertWeatherForPlan(plan);

        plan.start();
    }

    // 출발지 + 모든 배송지 좌표에 대해 날씨 데이터를 upsert
    private void upsertWeatherForPlan(DeliveryPlan plan) {
        WeatherUpdater.BaseDateTime baseDateTime = weatherUpdater.resolveLatestBaseDateTime();

        for (GridCoordinate coordinate : collectCoordinates(plan)) {
            try {
                weatherUpdater.update(WeatherRequest.builder()
                        .baseDate(baseDateTime.baseDate())
                        .baseTime(baseDateTime.baseTime())
                        .nx(coordinate.nx())
                        .ny(coordinate.ny())
                        .build());
            } catch (Exception e) {
                log.error("날씨 갱신 실패: nx={}, ny={}", coordinate.nx(), coordinate.ny(), e);
            }
        }
    }

    // 출발지 좌표 + 각 배송지 좌표를 격자좌표로 변환, 중복 제거
    private Set<GridCoordinate> collectCoordinates(DeliveryPlan plan) {
        Set<GridCoordinate> coordinates = new HashSet<>();
        coordinates.add(toGrid(plan.getDepartureLatitude(), plan.getDepartureLongitude()));
        for (DeliveryStop stop : plan.getDeliveryStops()) {
            coordinates.add(toGrid(stop.getLatitude(), stop.getLongitude()));
        }
        return coordinates;
    }

    private GridCoordinate toGrid(Double latitude, Double longitude) {
        LocationConverter.LatXLngY grid = LocationConverter.convertGridGps(
                LocationConverter.TO_GRID, latitude, longitude
        );
        return new GridCoordinate((int) grid.x, (int) grid.y);
    }

    @Transactional
    public void completeStop(
            Long planId,
            Long stopId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 포인트 배송 완료처리 요청 planId: {}, stopId: {}", PLAN, planId, stopId);
        plan.completeStop(stopId);
    }

    @Transactional
    public void completePlan(
            Long planId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 전체 배송 완료 처리 요청 planId: {}", PLAN, planId);
        plan.finish();
    }


    private DeliveryPlan getPlanIfExists(Long planId) {
        return deliveryPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_PLAN_NOT_FOUND));
    }
}
