package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryStop;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.dto.request.UpdateDeliveryOrderRequest;
import com.example.delivery_project.dto.request.UpdateScheduledDepartureRequest;
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

    public List<DeliveryPlanSummaryResponse> getDeliveryPlans(Long driverId) {
        List<DeliveryPlan> deliveryPlans = deliveryPlanRepository.findAllByDriverId(driverId);

        log.info("plan 목록 조회 완료 driverId: {}, planSize: {} ", driverId, deliveryPlans.size());
        return deliveryPlans.stream()
                .map(DeliveryPlanSummaryResponse::from)
                .toList();
    }

    public DeliveryPlanDetailResponse getDeliveryPlan(Long planId) {
        DeliveryPlan plan = getPlanIfExists(planId);
        return DeliveryPlanDetailResponse.from(plan);
    }

    public DeliveryStopResponse getDeliveryStop(
            Long planId,
            Long stopId
    ) {
        DeliveryStop stop = deliveryStopRepository.findDetailByIdAndPlanId(stopId, planId)
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_STOP_NOT_FOUND));

        return DeliveryStopResponse.from(stop);
    }

    @Transactional
    public void changeScheduledDepartureAt(
            Long planId,
            UpdateScheduledDepartureRequest request
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        plan.updateScheduledDepartureAt(request.scheduledDepartureAt());
    }

    @Transactional
    public void reorderStops(
            Long planId,
            UpdateDeliveryOrderRequest request
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        plan.reorderStops(request.stopIds());
    }

    @Transactional
    public void start(
            Long planId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        plan.start();
    }

    @Transactional
    public void completeStop(
            Long planId,
            Long stopId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        plan.completeStop(stopId);
    }

    @Transactional
    public void completePlan(
            Long planId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        plan.finish();
    }


    private DeliveryPlan getPlanIfExists(Long planId) {
        return deliveryPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_PLAN_NOT_FOUND));
    }
}
