package com.example.delivery_project.component;

import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.entity.delivery.RiskAssessment;

import java.time.LocalDateTime;

public interface RiskCalculator {
    RiskAssessment calculate(
            DeliveryStop stop,
            LocalDateTime targetTime
    );
}