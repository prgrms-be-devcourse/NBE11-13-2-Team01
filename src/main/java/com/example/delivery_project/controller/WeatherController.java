package com.example.delivery_project.controller;

import com.example.delivery_project.service.component.WeatherProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherProvider weatherProvider;

    @GetMapping
    public String test() throws Exception {
        return weatherProvider.getWeather();
    }


}
