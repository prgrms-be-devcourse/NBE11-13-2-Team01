package com.example.delivery_project.service.component;

import com.example.delivery_project.domain.entity.weather.Weather;
import com.example.delivery_project.domain.repository.WeatherRepository;
import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.dto.response.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherUpdater {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final WeatherProvider weatherProvider;
    private final WeatherRepository weatherRepository;

    public record BaseDateTime(String baseDate, String baseTime) {}

    // 초단기예보 발표시각(매시 30분) 기준, 현재 시각에서 조회 가능한 가장 최근 발표시각을 계산
    public BaseDateTime resolveLatestBaseDateTime() {
        LocalDateTime now = LocalDateTime.now(SEOUL);
        LocalDateTime availableReference = now.minusMinutes(15);
        LocalDateTime baseDateTime = availableReference
                .withMinute(30)
                .withSecond(0)
                .withNano(0);

        if (baseDateTime.isAfter(availableReference)) {
            baseDateTime = baseDateTime.minusHours(1);
        }
        return new BaseDateTime(
                baseDateTime.format(DATE_FORMATTER),
                baseDateTime.format(TIME_FORMATTER)
        );
    }

    //날씨 공공 데이터를 받아온다.
    public boolean update(WeatherRequest request) {
        //날씨 데이터 받아오기
        WeatherResponse response = weatherProvider.getWeather(request);

        if (!"00".equals(response.header().resultCode())) {
            log.info("weather api result: code={}, msg={}", response.header().resultCode(), response.header().resultMsg());
            //날씨 데이터를 받지 못했을 경우 처리
            return false;
        }

        LocalDateTime fetchedAt = LocalDateTime.now();
        for (WeatherResponse.Item item : response.body().items().item()) {
            upsert(item, fetchedAt);
        }
        return true;
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
