package com.example.delivery_project.controller;

import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.dto.response.WeatherResponse;
import com.example.delivery_project.service.WeatherService;
import com.example.delivery_project.service.component.WeatherProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherProvider weatherProvider;
    private final WeatherService weatherService;

    @GetMapping
    public WeatherResponse test(@RequestBody WeatherRequest request) throws Exception {
        return weatherProvider.getWeather(request);
    }
    //api 데이터 저장 테스트
    @GetMapping("/save")
    public void test2(@RequestBody WeatherRequest request){
        weatherService.save(request);
    }


}
