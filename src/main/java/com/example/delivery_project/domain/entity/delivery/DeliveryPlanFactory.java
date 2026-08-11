package com.example.delivery_project.domain.entity.delivery;

import com.example.delivery_project.component.RiskCalculator;
import com.example.delivery_project.domain.entity.delivery.spec.DeliveryItemSpec;
import com.example.delivery_project.domain.entity.delivery.spec.DeliveryStopSpec;
import com.example.delivery_project.domain.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeliveryPlanFactory {

    private final RiskCalculator riskCalculator;
    public DeliveryPlan create(
            User driver,
            String departureLocation,
            LocalDateTime scheduledDepartureAt,
            List<DeliveryStopSpec> stops
    ) {
        DeliveryPlan plan = DeliveryPlan.of(
                driver,
                departureLocation,
                scheduledDepartureAt
        );
        for(DeliveryStopSpec stopSpec : safeList(stops)) {
            DeliveryStop stop = plan.addStop(
                    stopSpec.address(),
                    stopSpec.latitude(),
                    stopSpec.longitude()
            );
            for(DeliveryItemSpec itemSpec : safeList(stopSpec.items())) {
                stop.addItem(
                        itemSpec.productName(),
                        itemSpec.productType(),
                        itemSpec.quantity()
                );
            }
            RiskAssessment assessment = riskCalculator.calculate(
                    stop,
                    scheduledDepartureAt
            );
            stop.attachRiskAssessment(assessment);
        }
        return plan;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
