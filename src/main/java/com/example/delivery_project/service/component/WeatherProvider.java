package com.example.delivery_project.service.component;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class WeatherProvider {

    private final RestClient restClient;

    @Value("${weather.api.key}")
    private String apiKey;

    public String getWeather() {

        String url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst"
                + "?serviceKey=" + apiKey.trim()
                + "&pageNo=1"
                + "&numOfRows=10"
                + "&dataType=JSON"
                + "&base_date=20260811"
                + "&base_time=1400"
                + "&nx=60"
                + "&ny=127";

        return restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(String.class);
    }
}
