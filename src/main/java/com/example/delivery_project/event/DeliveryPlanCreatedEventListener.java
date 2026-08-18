package com.example.delivery_project.event;

import com.example.delivery_project.service.DeliveryRiskRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryPlanCreatedEventListener {

    private final DeliveryRiskRefreshService deliveryRiskRefreshService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refreshRiskAfterPlanCreated(
            DeliveryPlanCreatedEvent event
    ) {
        try {
            deliveryRiskRefreshService.refreshPlan(event.planId());
        } catch (Exception e) {
            log.error(
                    "신규 배송 계획의 날씨·위험도 갱신 실패. planId={}",
                    event.planId(),
                    e
            );
        }
    }
}
