package com.example.delivery_project.controller;

import com.example.delivery_project.service.component.WeatherProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeatherControllerTest {

    @Test
    void weather_endpoint_delegates_to_provider() throws Exception {
        WeatherProvider provider = mock(WeatherProvider.class);
        when(provider.getWeather()).thenReturn("weather-response");
        WeatherController controller = new WeatherController(provider);

        String response = controller.test();

        assertThat(response).isEqualTo("weather-response");
        verify(provider).getWeather();
    }
}
