package com.example.delivery_project.service.component;

import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.domain.repository.WeatherRepository;
import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.dto.response.WeatherResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherUpdaterTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private WeatherProvider weatherProvider;

    @Mock
    private WeatherRepository weatherRepository;

    @InjectMocks
    private WeatherUpdater weatherUpdater;

    @Test
    void 정상_응답의_기존_날씨를_UPDATE한다() {
        WeatherRequest request = request();
        when(weatherProvider.getWeather(request))
                .thenReturn(successResponse());
        when(weatherRepository.updateFcstValue(
                anyInt(),
                anyInt(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalDate.class),
                any(LocalTime.class),
                any(String.class),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(1);

        boolean result = weatherUpdater.update(request);

        assertThat(result).isTrue();
        verify(weatherRepository).updateFcstValue(
                anyInt(),
                anyInt(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalDate.class),
                any(LocalTime.class),
                any(String.class),
                any(String.class),
                any(LocalDateTime.class)
        );
        verify(weatherRepository, never()).save(any());
    }

    @Test
    void 기존_날씨가_없으면_INSERT한다() {
        WeatherRequest request = request();
        when(weatherProvider.getWeather(request))
                .thenReturn(successResponse());
        when(weatherRepository.updateFcstValue(
                anyInt(),
                anyInt(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalDate.class),
                any(LocalTime.class),
                any(String.class),
                any(String.class),
                any(LocalDateTime.class)
        )).thenReturn(0);

        boolean result = weatherUpdater.update(request);

        ArgumentCaptor<Weather> weatherCaptor =
                ArgumentCaptor.forClass(Weather.class);
        verify(weatherRepository).save(weatherCaptor.capture());
        Weather savedWeather = weatherCaptor.getValue();

        assertThat(result).isTrue();
        assertThat(savedWeather.getNx()).isEqualTo(60);
        assertThat(savedWeather.getNy()).isEqualTo(127);
        assertThat(savedWeather.getBaseDate())
                .isEqualTo(LocalDate.of(2026, 8, 18));
        assertThat(savedWeather.getBaseTime())
                .isEqualTo(LocalTime.of(10, 30));
        assertThat(savedWeather.getFcstTime())
                .isEqualTo(LocalTime.of(11, 0));
        assertThat(savedWeather.getCategory()).isEqualTo("T1H");
        assertThat(savedWeather.getFcstValue()).isEqualTo("33");
    }

    @Test
    void 기상_API가_실패_코드를_반환하면_저장하지_않는다() {
        WeatherRequest request = request();
        when(weatherProvider.getWeather(request))
                .thenReturn(new WeatherResponse(
                        new WeatherResponse.Header("03", "NO_DATA"),
                        null
                ));

        boolean result = weatherUpdater.update(request);

        assertThat(result).isFalse();
        verifyNoInteractions(weatherRepository);
    }

    @Test
    void 아직_공개되지_않은_발표시각은_선택하지_않는다() {
        LocalDateTime lowerBound = LocalDateTime.now(SEOUL)
                .minusMinutes(76);

        WeatherUpdater.BaseDateTime result =
                weatherUpdater.resolveLatestBaseDateTime();

        LocalDateTime resolved = LocalDateTime.of(
                LocalDate.parse(
                        result.baseDate(),
                        DateTimeFormatter.BASIC_ISO_DATE
                ),
                LocalTime.parse(
                        result.baseTime(),
                        DateTimeFormatter.ofPattern("HHmm")
                )
        );
        LocalDateTime upperBound = LocalDateTime.now(SEOUL)
                .minusMinutes(14);

        assertThat(resolved.getMinute()).isEqualTo(30);
        assertThat(resolved).isBetween(lowerBound, upperBound);
    }

    private WeatherRequest request() {
        return WeatherRequest.builder()
                .baseDate("20260818")
                .baseTime("1030")
                .nx(60)
                .ny(127)
                .build();
    }

    private WeatherResponse successResponse() {
        WeatherResponse.Item item = new WeatherResponse.Item(
                "20260818",
                "1030",
                "T1H",
                "20260818",
                "1100",
                "33",
                60,
                127
        );

        return new WeatherResponse(
                new WeatherResponse.Header("00", "NORMAL_SERVICE"),
                new WeatherResponse.Body(
                        "JSON",
                        new WeatherResponse.Items(List.of(item)),
                        1,
                        10,
                        1
                )
        );
    }
}
