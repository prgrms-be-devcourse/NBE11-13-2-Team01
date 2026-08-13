package com.example.delivery_project.service.component;

import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.dto.response.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WeatherProvider {

    private final RestClient restClient;

    @Value("${weather.api.key}")
    private String apiKey;

    public WeatherResponse getWeather(WeatherRequest request) {

        URI uri = UriComponentsBuilder
                        .fromUriString("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst")
                        .queryParam("serviceKey",apiKey.trim())
                        .queryParam("pageNo",1)
                        .queryParam("numOfRows",6000)
                        .queryParam("dataType","JSON")
                        .queryParam("base_date",request.baseDate())
                        .queryParam("base_time",request.baseTime())
                        .queryParam("nx",request.nx())
                        .queryParam("ny",request.ny())
                        .build(true)
                .toUri();

        Map<String, WeatherResponse> response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, WeatherResponse>>() {
                });

        return response.get("response");

    }
}
