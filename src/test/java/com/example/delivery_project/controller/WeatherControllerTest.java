package com.example.delivery_project.controller;

import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.dto.response.WeatherResponse;
import com.example.delivery_project.service.WeatherService;
import com.example.delivery_project.service.component.WeatherProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    @Mock
    private WeatherProvider weatherProvider;

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController controller;

    @Test
    void 날씨_API_테스트_응답을_반환한다() throws Exception {
        WeatherRequest request = request();
        WeatherResponse response = new WeatherResponse(
                new WeatherResponse.Header("00", "NORMAL"),
                null
        );
        when(weatherProvider.getWeather(request)).thenReturn(response);

        assertThat(controller.test(request)).isEqualTo(response);
    }

    @Test
    void 날씨_저장_요청을_서비스에_전달한다() {
        WeatherRequest request = request();

        controller.test2(request);

        verify(weatherService).save(request);
    }

    private WeatherRequest request() {
        return WeatherRequest.builder()
                .baseDate("20260818")
                .baseTime("1030")
                .nx(60)
                .ny(127)
                .build();
    }
}
