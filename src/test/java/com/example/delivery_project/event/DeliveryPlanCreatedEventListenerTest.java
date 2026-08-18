package com.example.delivery_project.event;

import com.example.delivery_project.service.DeliveryRiskRefreshService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanCreatedEventListenerTest {

    @Mock
    private DeliveryRiskRefreshService deliveryRiskRefreshService;

    @InjectMocks
    private DeliveryPlanCreatedEventListener listener;

    @Test
    void 계획_생성_커밋_후_해당_계획의_위험도를_갱신한다() {
        listener.refreshRiskAfterPlanCreated(
                new DeliveryPlanCreatedEvent(10L)
        );

        verify(deliveryRiskRefreshService).refreshPlan(10L);
    }

    @Test
    void 생성_후_갱신_실패는_계획_생성에_전파하지_않는다() {
        doThrow(new IllegalStateException("갱신 실패"))
                .when(deliveryRiskRefreshService)
                .refreshPlan(10L);

        assertThatCode(() -> listener.refreshRiskAfterPlanCreated(
                new DeliveryPlanCreatedEvent(10L)
        )).doesNotThrowAnyException();
    }
}
