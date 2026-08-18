package com.example.delivery_project.scheduler;

import com.example.delivery_project.service.DeliveryRiskRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WeatherBatchScheduler {

    private final DeliveryRiskRefreshService deliveryRiskRefreshService;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refresh("애플리케이션 시작");
    }

    @Scheduled(cron = "0 45 * * * *", zone = "Asia/Seoul")
    public void refreshHourly() {
        refresh("매시 45분 배치");
    }

    private void refresh(String trigger) {
        log.info("[날씨·위험도 갱신 시작] trigger={}", trigger);
        try {
            deliveryRiskRefreshService.refreshActiveStops();
        } catch (Exception e) {
            log.error("[날씨·위험도 갱신 실패] trigger={}", trigger, e);
        }
    }
}
