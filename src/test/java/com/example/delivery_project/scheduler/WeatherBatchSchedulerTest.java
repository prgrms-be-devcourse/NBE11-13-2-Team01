package com.example.delivery_project.scheduler;

import com.example.delivery_project.service.DeliveryRiskRefreshService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeatherBatchSchedulerTest {

    @Mock
    private DeliveryRiskRefreshService deliveryRiskRefreshService;

    @InjectMocks
    private WeatherBatchScheduler scheduler;

    @Test
    void 애플리케이션_시작과_매시_45분에_활성_배송지를_갱신한다() {
        scheduler.refreshOnStartup();
        scheduler.refreshHourly();

        verify(deliveryRiskRefreshService, times(2))
                .refreshActiveStops();
    }

    @Test
    void 배치_실패가_스케줄러_밖으로_전파되지_않는다() {
        doThrow(new IllegalStateException("갱신 실패"))
                .when(deliveryRiskRefreshService)
                .refreshActiveStops();

        assertThatCode(scheduler::refreshHourly)
                .doesNotThrowAnyException();
    }
}
