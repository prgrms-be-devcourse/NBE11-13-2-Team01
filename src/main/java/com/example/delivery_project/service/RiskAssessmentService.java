package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.domain.repository.GridCoordinate;
import com.example.delivery_project.domain.repository.RiskAssessmentRepository;
import com.example.delivery_project.domain.repository.WeatherRepository;
import com.example.delivery_project.exception.RiskException;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.service.component.DemoRiskScenarioPolicy;
import com.example.delivery_project.service.component.LocationConverter;
import com.example.delivery_project.service.component.RiskFactorCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskAssessmentService {

    private static final List<String> RISK_CATEGORIES =
            List.of("T1H", "RN1", "PTY");
    private static final long WEATHER_FALLBACK_HOURS = 2;

    private final WeatherRepository weatherRepository;
    private final RiskFactorCalculator riskFactorCalculator;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final DemoRiskScenarioPolicy demoRiskScenarioPolicy;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAssessments(
            List<DeliveryStop> deliveryStops
    ) {
        if (deliveryStops.isEmpty()) {
            return;
        }

        Map<Long, RiskAssessment> assessmentsByStopId = riskAssessmentRepository
                .findAllWithFactorsByDeliveryStopIdIn(
                        deliveryStops.stream()
                                .map(DeliveryStop::getId)
                                .toList()
                ).stream().collect(Collectors.toMap(
                        assessment -> assessment
                                .getDeliveryStop()
                                .getId(),
                        Function.identity()
                ));
        LocalDateTime analyzedAt = LocalDateTime.now();
        List<AssessmentTarget> weatherTargets = new ArrayList<>();

        for (DeliveryStop stop : deliveryStops) {
            RiskAssessment assessment = Optional.ofNullable(
                            assessmentsByStopId.get(stop.getId())
                    ).orElseThrow(() -> new BusinessException(
                            RiskException.RISK_NOT_FOUND
                    ));

            if (demoRiskScenarioPolicy.applyIfEnabled(
                    stop.getId(),
                    assessment,
                    analyzedAt
            )) {
                log.info("데모 위험도 시나리오를 적용합니다. stopId={}, level={}, score={}",
                        stop.getId(),
                        assessment.getLevel(),
                        assessment.getScore()
                );
                continue;
            }

            weatherTargets.add(new AssessmentTarget(stop, assessment));
        }

        if (weatherTargets.isEmpty()) {
            return;
        }

        Map<GridCoordinate, Optional<Map<String, String>>> weatherByCoordinate =
                fetchWeatherValues(weatherTargets, analyzedAt);

        for (AssessmentTarget target : weatherTargets) {
            DeliveryStop stop = target.stop();
            RiskAssessment assessment = target.assessment();
            Optional<Map<String, String>> weatherValues = weatherByCoordinate.getOrDefault(
                    toGrid(stop),
                    Optional.empty()
            );

            if (weatherValues.isEmpty()) {
                assessment.markUnknown(analyzedAt);
                log.warn(
                        "사용 가능한 날씨 데이터가 없어 위험도를 UNKNOWN으로 변경합니다. stopId={}",
                        stop.getId()
                );
                continue;
            }

            List<RiskFactorType> types =
                    calculateRiskFactorTypes(weatherValues.get());

            assessment.replaceFactors(types, analyzedAt);
        }
    }

    private List<RiskFactorType> calculateRiskFactorTypes(
            Map<String, String> weatherValues
    ) {
        List<RiskFactorType> types = new ArrayList<>();

        if (riskFactorCalculator.isHeavyRain(
                weatherValues.get("RN1"),
                weatherValues.get("PTY")
        )) {
            types.add(RiskFactorType.HEAVY_RAIN);
        }

        if (riskFactorCalculator.isHeatWave(
                weatherValues.get("T1H")
        )) {
            types.add(RiskFactorType.HEAT_WAVE);
        }

        return types;
    }

    // 현재 예보 시각부터 직전 2시간 이내의 가장 최신인 완전한 데이터 세트를 조회한다.
    private Map<GridCoordinate, Optional<Map<String, String>>> fetchWeatherValues(
            List<AssessmentTarget> targets,
            LocalDateTime now
    ) {
        Set<GridCoordinate> coordinates = targets.stream()
                .map(target -> toGrid(target.stop()))
                .collect(Collectors.toSet());

        LocalDateTime currentForecastAt = now.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime earliestForecastAt =
                currentForecastAt.minusHours(WEATHER_FALLBACK_HOURS);

        List<Weather> weathers = weatherRepository
                .findByNxInAndNyInAndFcstDateBetweenAndCategoryIn(
                        coordinates.stream()
                                .map(GridCoordinate::nx)
                                .collect(Collectors.toSet()),
                        coordinates.stream()
                                .map(GridCoordinate::ny)
                                .collect(Collectors.toSet()),
                        earliestForecastAt.toLocalDate(),
                        currentForecastAt.toLocalDate(),
                        RISK_CATEGORIES
                );

        Map<GridCoordinate, List<Weather>> weathersByCoordinate = weathers.stream()
                .filter(weather -> coordinates.contains(new GridCoordinate(
                        weather.getNx(),
                        weather.getNy()
                )))
                .collect(Collectors.groupingBy(weather -> new GridCoordinate(
                        weather.getNx(),
                        weather.getNy()
                )));

        Map<GridCoordinate, Optional<Map<String, String>>> result = new HashMap<>();
        for (GridCoordinate coordinate : coordinates) {
            result.put(
                    coordinate,
                    selectLatestWeatherValues(
                            weathersByCoordinate.getOrDefault(
                                    coordinate,
                                    List.of()
                            ),
                            earliestForecastAt,
                            currentForecastAt
                    )
            );
        }
        return result;
    }

    private Optional<Map<String, String>> selectLatestWeatherValues(
            List<Weather> weathers,
            LocalDateTime earliestForecastAt,
            LocalDateTime currentForecastAt
    ) {
        Map<LocalDateTime, Map<String, String>> valuesByForecastAt =
                weathers.stream()
                        .collect(Collectors.groupingBy(
                                weather -> LocalDateTime.of(
                                        weather.getFcstDate(),
                                        weather.getFcstTime()
                                ),
                                Collectors.toMap(
                                        Weather::getCategory,
                                        Weather::getFcstValue,
                                        (existing, replacement) -> replacement
                                )
                        ));

        return valuesByForecastAt.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(earliestForecastAt))
                .filter(entry -> !entry.getKey().isAfter(currentForecastAt))
                .filter(entry -> entry.getValue().keySet().containsAll(RISK_CATEGORIES))
                .max(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(Map.Entry::getValue);
    }

    private GridCoordinate toGrid(DeliveryStop stop) {
        LocationConverter.LatXLngY grid = LocationConverter.convertGridGps(
                LocationConverter.TO_GRID,
                stop.getLatitude(),
                stop.getLongitude()
        );
        return new GridCoordinate((int) grid.x, (int) grid.y);
    }

    private record AssessmentTarget(
            DeliveryStop stop,
            RiskAssessment assessment
    ) {}
}
