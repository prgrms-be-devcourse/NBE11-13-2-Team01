package com.example.delivery_project.domain.repository;

import com.example.delivery_project.domain.entity.delivery.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    @Query("""
        select distinct assessment
        from RiskAssessment assessment
        join fetch assessment.deliveryStop stop
        left join fetch assessment.riskFactors
        where stop.id in :stopIds
    """)
    List<RiskAssessment> findAllWithFactorsByDeliveryStopIdIn(
            Collection<Long> stopIds
    );

    @Query("""
        select distinct assessment
        from RiskAssessment assessment
        left join fetch assessment.riskFactors
        where assessment.deliveryStop.deliveryPlan.id = :planId
    """)
    List<RiskAssessment> findAllWithFactorsByDeliveryPlanId(Long planId);
}
