package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    Optional<RiskAssessment> findByDeliveryStopId(Long deliveryStopId);
}
