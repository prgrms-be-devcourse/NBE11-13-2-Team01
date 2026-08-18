package com.example.delivery_project.service;

import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.service.component.WeatherUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherUpdater weatherUpdater;

    @InjectMocks
    private WeatherService weatherService;

    @Test
    void 날씨_저장_결과를_반환한다() {
        WeatherRequest request = WeatherRequest.builder()
                .baseDate("20260818")
                .baseTime("1030")
                .nx(60)
                .ny(127)
                .build();
        when(weatherUpdater.update(request)).thenReturn(true);

        assertThat(weatherService.save(request)).isTrue();
        verify(weatherUpdater).update(request);
    }

    @Test
    void 최신_발표시각_계산을_위임한다() {
        WeatherUpdater.BaseDateTime baseDateTime =
                new WeatherUpdater.BaseDateTime("20260818", "1030");
        when(weatherUpdater.resolveLatestBaseDateTime())
                .thenReturn(baseDateTime);

        assertThat(weatherService.resolveLatestBaseDateTime())
                .isEqualTo(baseDateTime);
    }
}
