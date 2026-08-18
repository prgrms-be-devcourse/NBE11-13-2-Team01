package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.dto.request.WeatherRequest;
import com.example.delivery_project.service.component.WeatherUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryRiskRefreshServiceTest {

    @Mock
    private DeliveryStopRepository deliveryStopRepository;

    @Mock
    private WeatherService weatherService;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    @Mock
    private DeliveryStop firstStop;

    @Mock
    private DeliveryStop secondStop;

    @InjectMocks
    private DeliveryRiskRefreshService deliveryRiskRefreshService;

    @BeforeEach
    void setUp() {
        when(weatherService.resolveLatestBaseDateTime())
                .thenReturn(new WeatherUpdater.BaseDateTime(
                        "20260818",
                        "1030"
                ));
    }

    @Test
    void 같은_격자의_배송지는_날씨를_한_번만_갱신한다() {
        givenSameLocationStops();
        when(weatherService.save(any(WeatherRequest.class)))
                .thenReturn(true);

        deliveryRiskRefreshService.refreshStops(
                List.of(firstStop, secondStop)
        );

        verify(weatherService, times(1))
                .save(any(WeatherRequest.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeliveryStop>> stopsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(riskAssessmentService)
                .updateAssessments(stopsCaptor.capture());

        assertThat(stopsCaptor.getValue())
                .containsExactlyInAnyOrder(firstStop, secondStop);
    }

    @Test
    void 날씨_API가_실패해도_저장된_데이터로_위험도_갱신을_시도한다() {
        givenSameLocationStops();
        when(weatherService.save(any(WeatherRequest.class)))
                .thenThrow(new IllegalStateException("기상 API 실패"));

        deliveryRiskRefreshService.refreshStops(
                List.of(firstStop, secondStop)
        );

        verify(riskAssessmentService)
                .updateAssessments(any());
    }

    private void givenSameLocationStops() {
        when(firstStop.getLatitude()).thenReturn(37.5665);
        when(firstStop.getLongitude()).thenReturn(126.9780);

        when(secondStop.getLatitude()).thenReturn(37.5665);
        when(secondStop.getLongitude()).thenReturn(126.9780);
    }
}
