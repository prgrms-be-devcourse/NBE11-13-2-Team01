package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.domain.repository.RiskAssessmentRepository;
import com.example.delivery_project.dto.request.UpdateDeliveryOrderRequest;
import com.example.delivery_project.dto.request.UpdateScheduledDepartureRequest;
import com.example.delivery_project.dto.projection.DeliveryPlanSummaryProjection;
import com.example.delivery_project.dto.response.DeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.DeliveryPlanSummaryResponse;
import com.example.delivery_project.dto.response.DeliveryStopResponse;
import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.global.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DeliveryPlanService {
    private final DeliveryPlanRepository deliveryPlanRepository;
    private final DeliveryStopRepository deliveryStopRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    private static final String PLAN = "[PLAN]";
    private static final String STOP = "[STOP]";

    public List<DeliveryPlanSummaryResponse> getDeliveryPlans(Long driverId) {
        List<DeliveryPlanSummaryProjection> deliveryPlans =
                deliveryPlanRepository.findAllSummariesByDriverId(driverId);

        log.info("{} 목록 조회 완료 driverId: {}, planSize: {} ", PLAN, driverId, deliveryPlans.size());
        return deliveryPlans.stream()
                .map(DeliveryPlanSummaryResponse::from)
                .toList();
    }

    public DeliveryPlanDetailResponse getDeliveryPlan(
            Long planId,
            Long driverId
    ) {
        DeliveryPlan plan = getOwnedPlanWithStopsAndRisk(planId, driverId);
        initializePlanDetail(planId);

        log.info("{} 조회 완료 planId: {}", PLAN, planId);
        return DeliveryPlanDetailResponse.from(plan);
    }

    public DeliveryStopResponse getDeliveryStop(
            Long planId,
            Long stopId,
            Long driverId
    ) {
        getOwnedPlan(planId, driverId);

        DeliveryStop stop = deliveryStopRepository.findDetailByIdAndPlanId(stopId, planId)
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_STOP_NOT_FOUND));
        riskAssessmentRepository.findAllWithFactorsByDeliveryStopIdIn(
                List.of(stopId)
        );
        log.info("{} 조회 완료 planId: {}, stopId: {}", STOP, planId, stopId);

        return DeliveryStopResponse.from(stop);
    }

    @Transactional
    public void changeScheduledDepartureAt(
            Long planId,
            Long driverId,
            UpdateScheduledDepartureRequest request
    ) {
        DeliveryPlan plan = getOwnedPlan(planId, driverId);
        log.info("{} 예정 시간 변경 요청 planId: {}, 변경 요청 시간: {}", PLAN, planId, request.scheduledDepartureAt());
        plan.updateScheduledDepartureAt(request.scheduledDepartureAt());
    }

    @Transactional
    public void reorderStops(
            Long planId,
            Long driverId,
            UpdateDeliveryOrderRequest request
    ) {
        DeliveryPlan plan = getOwnedPlanWithStopsAndRisk(planId, driverId);
        log.info("{} 순서 편집 요청 planId: {}", PLAN, planId);
        plan.reorderStops(request.stopIds());
    }

    @Transactional
    public void start(
            Long planId,
            Long driverId
    ) {
        DeliveryPlan plan = getOwnedPlanWithStopsAndRisk(planId, driverId);
        log.info("{} 배송 시작 요청 planId: {}", PLAN, planId);
        plan.start();
    }

    @Transactional
    public void completeStop(
            Long planId,
            Long stopId,
            Long driverId
    ) {
        DeliveryPlan plan = getOwnedPlanWithStopsAndRisk(planId, driverId);
        log.info("{} 포인트 배송 완료처리 요청 planId: {}, stopId: {}", PLAN, planId, stopId);
        plan.completeStop(stopId);
    }

    @Transactional
    public void completePlan(
            Long planId,
            Long driverId
    ) {
        DeliveryPlan plan = getOwnedPlanWithStopsAndRisk(planId, driverId);
        log.info("{} 전체 배송 완료 처리 요청 planId: {}", PLAN, planId);
        plan.finish();
    }


    private DeliveryPlan getOwnedPlan(
            Long planId,
            Long driverId
    ) {
        return deliveryPlanRepository.findByIdAndDriverId(
                        planId,
                        driverId
                )
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_PLAN_NOT_FOUND));
    }

    private DeliveryPlan getOwnedPlanWithStopsAndRisk(
            Long planId,
            Long driverId
    ) {
        return deliveryPlanRepository
                .findWithStopsAndRiskByIdAndDriverId(planId, driverId)
                .orElseThrow(() -> new BusinessException(
                        DeliveryException.DELIVERY_PLAN_NOT_FOUND
                ));
    }

    private void initializePlanDetail(Long planId) {
        deliveryStopRepository.findAllWithItemsByDeliveryPlanId(planId);
        riskAssessmentRepository.findAllWithFactorsByDeliveryPlanId(planId);
    }
}
