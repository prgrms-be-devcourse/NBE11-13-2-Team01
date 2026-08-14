package com.example.delivery_project.scheduler;

import com.example.delivery_project.domain.repository.GridCoordinate;
import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class WeatherBatchScheduler {

    private final WeatherService weatherService;

    @Scheduled(cron="0 45 * * * *",zone="Asia/Seoul")
    public void upsertWeather(){
        log.info("[초단기예보 업데이트 시작]");
        //1. 현재 db의 모든 nx,ny좌표에 대해서 현재 날짜/시간 기준으로 데이터를 다시 받아온다
        //2. update한다.
        LocalDateTime baseDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .withMinute(30).withSecond(0).withNano(0);

        String baseTime= baseDateTime.format(DateTimeFormatter.ofPattern("HHmm")); //hhmm 형식
        String baseDate=baseDateTime.format(DateTimeFormatter.BASIC_ISO_DATE); //yyyymmdd 형식

        List<GridCoordinate> gridCoordinates = weatherService.getGridCoordinates();

        //모든 격자좌표에 대해서 업데이트
        for(GridCoordinate coordinate : gridCoordinates){
            int nx=coordinate.nx();
            int ny=coordinate.ny();

            weatherService.save(WeatherRequest.builder()
                    .baseDate(baseDate)
                    .baseTime(baseTime)
                    .nx(nx)
                    .ny(ny)
                    .build());
        }

    }
}
