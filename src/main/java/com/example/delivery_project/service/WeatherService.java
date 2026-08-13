package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.domain.repository.WeatherRepository;
import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.dto.response.WeatherResponse;
import com.example.delivery_project.service.component.WeatherProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WeatherService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    private final WeatherProvider weatherProvider;
    private final WeatherRepository weatherRepository;

    @Transactional
    public void save(WeatherRequest request) {
        //날씨 데이터 받아오기
        WeatherResponse response = weatherProvider.getWeather(request);

        if (!"00".equals(response.header().resultCode())) {
            // TODO 실패 처리(로깅/재시도)로 교체
            return;
        }

        LocalDateTime fetchedAt = LocalDateTime.now();
        for (WeatherResponse.Item item : response.body().items().item()) {
            upsert(item, fetchedAt);
        }
    }

    //해당 데이터가 있으면 Update, 없으면 Insert
    private void upsert(WeatherResponse.Item item, LocalDateTime fetchedAt) {
        LocalDate fcstDate = LocalDate.parse(item.fcstDate(), DATE_FORMATTER);
        LocalTime fcstTime = LocalTime.parse(item.fcstTime(), TIME_FORMATTER);
        LocalDate baseDate = LocalDate.parse(item.baseDate(), DATE_FORMATTER);
        LocalTime baseTime = LocalTime.parse(item.baseTime(), TIME_FORMATTER);

        int updated = weatherRepository.updateFcstValue(
                item.nx(), item.ny(), fcstDate, fcstTime,
                baseDate, baseTime, item.category(), item.fcstValue(), fetchedAt
        );

        if (updated == 0) {
            weatherRepository.save(Weather.of(
                    item.nx(), item.ny(), fcstDate, fcstTime,
                    baseDate, baseTime, item.category(), item.fcstValue()
            ));
        }
    }
}
