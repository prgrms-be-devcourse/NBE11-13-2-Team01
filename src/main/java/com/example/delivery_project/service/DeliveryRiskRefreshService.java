package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.domain.repository.GridCoordinate;
import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.enums.DeliveryStopStatus;
import com.example.delivery_project.service.component.LocationConverter;
import com.example.delivery_project.service.component.WeatherUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryRiskRefreshService {

    private static final List<DeliveryStopStatus> ACTIVE_STATUSES =
            List.of(
                    DeliveryStopStatus.READY,
                    DeliveryStopStatus.DELIVERING
            );

    private final DeliveryStopRepository deliveryStopRepository;
    private final WeatherService weatherService;
    private final RiskAssessmentService riskAssessmentService;

    public void refreshActiveStops() {
        refreshStops(
                deliveryStopRepository.findAllByStatusIn(ACTIVE_STATUSES)
        );
    }

    public void refreshPlan(Long planId) {
        refreshStops(
                deliveryStopRepository
                        .findAllByDeliveryPlanIdAndStatusIn(
                                planId,
                                ACTIVE_STATUSES
                        )
        );
    }

    public void refreshStops(Collection<DeliveryStop> stops) {
        if (stops.isEmpty()) {
            log.info("갱신할 활성 배송지가 없습니다.");
            return;
        }

        WeatherUpdater.BaseDateTime baseDateTime =
                weatherService.resolveLatestBaseDateTime();

        Map<GridCoordinate, List<DeliveryStop>> stopsByCoordinate =
                stops.stream()
                        .collect(Collectors.groupingBy(this::toGrid));

        log.info(
                "배송 위험도 갱신 시작. stopCount={}, coordinateCount={}",
                stops.size(),
                stopsByCoordinate.size()
        );

        for (Map.Entry<GridCoordinate, List<DeliveryStop>> entry
                : stopsByCoordinate.entrySet()) {
            refreshCoordinate(
                    entry.getKey(),
                    entry.getValue(),
                    baseDateTime
            );
        }
    }

    private void refreshCoordinate(
            GridCoordinate coordinate,
            List<DeliveryStop> stops,
            WeatherUpdater.BaseDateTime baseDateTime
    ) {
        updateWeather(coordinate, baseDateTime);

        try {
            riskAssessmentService.updateAssessments(stops);
        } catch (Exception e) {
            log.error(
                    "배송지 위험도 갱신 실패. nx={}, ny={}, stopIds={}",
                    coordinate.nx(),
                    coordinate.ny(),
                    stops.stream().map(DeliveryStop::getId).toList(),
                    e
            );
        }
    }

    private void updateWeather(
            GridCoordinate coordinate,
            WeatherUpdater.BaseDateTime baseDateTime
    ) {
        try {
            boolean updated = weatherService.save(
                    WeatherRequest.builder()
                            .baseDate(baseDateTime.baseDate())
                            .baseTime(baseDateTime.baseTime())
                            .nx(coordinate.nx())
                            .ny(coordinate.ny())
                            .build()
            );

            if (!updated) {
                log.warn(
                        "기상 API가 실패 응답을 반환했습니다. 저장된 데이터를 사용합니다. nx={}, ny={}",
                        coordinate.nx(),
                        coordinate.ny()
                );
            }
        } catch (Exception e) {
            log.error(
                    "날씨 갱신 실패. 저장된 데이터를 사용합니다. nx={}, ny={}",
                    coordinate.nx(),
                    coordinate.ny(),
                    e
            );
        }
    }

    private GridCoordinate toGrid(DeliveryStop stop) {
        LocationConverter.LatXLngY grid =
                LocationConverter.convertGridGps(
                        LocationConverter.TO_GRID,
                        stop.getLatitude(),
                        stop.getLongitude()
                );

        return new GridCoordinate(
                (int) grid.x,
                (int) grid.y
        );
    }
}
