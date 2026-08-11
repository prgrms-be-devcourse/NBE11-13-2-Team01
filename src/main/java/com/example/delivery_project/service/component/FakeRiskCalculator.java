package com.example.delivery_project.service.component;

import com.example.delivery_project.component.RiskCalculator;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class FakeRiskCalculator implements RiskCalculator {
    @Override
    public RiskAssessment calculate(DeliveryStop stop, LocalDateTime targetTime) {
        return null;
    }
}
