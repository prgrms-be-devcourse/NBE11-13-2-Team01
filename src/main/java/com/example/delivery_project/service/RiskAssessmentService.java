package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.domain.repository.RiskAssessmentRepository;
import com.example.delivery_project.domain.repository.WeatherRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAssessments(
            List<DeliveryStop> deliveryStops
    ) {
        for (DeliveryStop stop : deliveryStops) {
            updateAssessment(stop);
        }
    }

    private void updateAssessment(DeliveryStop stop) {
        RiskAssessment assessment = riskAssessmentRepository
                .findByDeliveryStopId(stop.getId())
                //TODO 커스텀 에외로 변경
                .orElseThrow(() -> new IllegalStateException(
                        "위험도 평가가 존재하지 않습니다. stopId="
                                + stop.getId()
                ));

        LocalDateTime analyzedAt = LocalDateTime.now();
        Optional<Map<String, String>> weatherValues =
                fetchWeatherValues(stop, analyzedAt);

        if (weatherValues.isEmpty()) {
            assessment.markUnknown(analyzedAt);
            log.warn(
                    "사용 가능한 날씨 데이터가 없어 위험도를 UNKNOWN으로 변경합니다. stopId={}",
                    stop.getId()
            );
            return;
        }

        List<RiskFactorType> types =
                calculateRiskFactorTypes(weatherValues.get());

        assessment.replaceFactors(
                types,
                analyzedAt
        );
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
    private Optional<Map<String, String>> fetchWeatherValues(
            DeliveryStop deliveryStop,
            LocalDateTime now
    ) {
        LocationConverter.LatXLngY grid = LocationConverter.convertGridGps(
                LocationConverter.TO_GRID,
                deliveryStop.getLatitude(),
                deliveryStop.getLongitude()
        );
        int nx = (int) grid.x;
        int ny = (int) grid.y;

        LocalDateTime currentForecastAt = now.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime earliestForecastAt =
                currentForecastAt.minusHours(WEATHER_FALLBACK_HOURS);

        List<Weather> weathers = weatherRepository
                .findByNxAndNyAndFcstDateBetweenAndCategoryIn(
                        nx,
                        ny,
                        earliestForecastAt.toLocalDate(),
                        currentForecastAt.toLocalDate(),
                        RISK_CATEGORIES
                );

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
}
