package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import com.example.delivery_project.domain.entity.delivery.RiskFactor;
import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.domain.repository.RiskAssessmentRepository;
import com.example.delivery_project.domain.repository.WeatherRepository;
import com.example.delivery_project.enums.RiskFactorType;
import com.example.delivery_project.enums.RiskLevel;
import com.example.delivery_project.service.component.RiskFactorCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private RiskFactorCalculator riskFactorCalculator;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private DeliveryStop stop;

    @InjectMocks
    private RiskAssessmentService riskAssessmentService;

    @Test
    void 현재_데이터가_불완전하면_2시간_이내의_완전한_데이터를_사용한다() {
        LocalDateTime currentForecastAt = LocalDateTime.now()
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime fallbackForecastAt = currentForecastAt.minusHours(1);

        RiskAssessment assessment = RiskAssessment.of(
                null,
                LocalDateTime.now()
        );
        givenStopAndAssessment(assessment);

        List<Weather> weathers = List.of(
                weather(currentForecastAt, "T1H", "21"),
                weather(fallbackForecastAt, "T1H", "20"),
                weather(fallbackForecastAt, "RN1", "0"),
                weather(fallbackForecastAt, "PTY", "0")
        );

        when(weatherRepository
                .findByNxAndNyAndFcstDateBetweenAndCategoryIn(
                        anyInt(),
                        anyInt(),
                        any(),
                        any(),
                        any()
                ))
                .thenReturn(weathers);
        when(riskFactorCalculator.isHeavyRain("0", "0"))
                .thenReturn(false);
        when(riskFactorCalculator.isHeatWave("20"))
                .thenReturn(false);

        riskAssessmentService.updateAssessments(List.of(stop));

        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.SAFE);
        assertThat(assessment.getScore()).isZero();
    }

    @Test
    void 직전_2시간_이내의_완전한_날씨가_없으면_UNKNOWN으로_변경한다() {
        RiskAssessment assessment = RiskAssessment.of(
                null,
                LocalDateTime.now()
        );
        assessment.replaceFactors(
                List.of(RiskFactorType.HEAVY_RAIN),
                LocalDateTime.now()
        );
        givenStopAndAssessment(assessment);

        when(weatherRepository
                .findByNxAndNyAndFcstDateBetweenAndCategoryIn(
                        anyInt(),
                        anyInt(),
                        any(),
                        any(),
                        any()
                ))
                .thenReturn(List.of());

        riskAssessmentService.updateAssessments(List.of(stop));

        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.UNKNOWN);
        assertThat(assessment.getScore()).isEqualTo(-1);
        assertThat(assessment.getRiskFactors()).isEmpty();
    }

    @Test
    void 현재_날씨가_폭우와_폭염이면_두_위험요인을_반영한다() {
        LocalDateTime forecastAt = LocalDateTime.now()
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        RiskAssessment assessment = RiskAssessment.of(
                null,
                LocalDateTime.now()
        );
        givenStopAndAssessment(assessment);

        List<Weather> weathers = List.of(
                weather(forecastAt, "T1H", "35"),
                weather(forecastAt, "RN1", "30mm 이상"),
                weather(forecastAt, "PTY", "1")
        );
        when(weatherRepository
                .findByNxAndNyAndFcstDateBetweenAndCategoryIn(
                        anyInt(),
                        anyInt(),
                        any(),
                        any(),
                        any()
                ))
                .thenReturn(weathers);
        when(riskFactorCalculator.isHeavyRain("30mm 이상", "1"))
                .thenReturn(true);
        when(riskFactorCalculator.isHeatWave("35"))
                .thenReturn(true);

        riskAssessmentService.updateAssessments(List.of(stop));

        assertThat(assessment.getRiskFactors())
                .extracting(RiskFactor::getType)
                .containsExactly(
                        RiskFactorType.HEAVY_RAIN,
                        RiskFactorType.HEAT_WAVE
                );
        assertThat(assessment.getScore()).isEqualTo(50);
        assertThat(assessment.getLevel()).isEqualTo(RiskLevel.CAUTION);
    }

    @Test
    void 배송지의_위험도_평가가_없으면_예외가_발생한다() {
        when(stop.getId()).thenReturn(999L);
        when(riskAssessmentRepository.findByDeliveryStopId(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> riskAssessmentService.updateAssessments(List.of(stop))
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stopId=999");

        verifyNoInteractions(weatherRepository, riskFactorCalculator);
    }

    private void givenStopAndAssessment(RiskAssessment assessment) {
        when(stop.getId()).thenReturn(1L);
        when(stop.getLatitude()).thenReturn(37.5665);
        when(stop.getLongitude()).thenReturn(126.9780);
        when(riskAssessmentRepository.findByDeliveryStopId(eq(1L)))
                .thenReturn(Optional.of(assessment));
    }

    private Weather weather(
            LocalDateTime forecastAt,
            String category,
            String value
    ) {
        Weather weather = mock(Weather.class);
        when(weather.getFcstDate()).thenReturn(forecastAt.toLocalDate());
        when(weather.getFcstTime()).thenReturn(forecastAt.toLocalTime());
        when(weather.getCategory()).thenReturn(category);
        when(weather.getFcstValue()).thenReturn(value);
        return weather;
    }
}
