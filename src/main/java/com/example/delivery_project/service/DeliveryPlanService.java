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

    private static final String PLAN = "[PLAN]";
    private static final String STOP = "[STOP]";

    public List<DeliveryPlanSummaryResponse> getDeliveryPlans(Long driverId) {
        List<DeliveryPlan> deliveryPlans = deliveryPlanRepository.findAllByDriverId(driverId);

        log.info("{} 목록 조회 완료 driverId: {}, planSize: {} ", PLAN, driverId, deliveryPlans.size());
        return deliveryPlans.stream()
                .map(DeliveryPlanSummaryResponse::from)
                .toList();
    }

    public DeliveryPlanDetailResponse getDeliveryPlan(Long planId) {
        DeliveryPlan plan = getPlanIfExists(planId);

        log.info("{} 조회 완료 planId: {}", PLAN, planId);
        return DeliveryPlanDetailResponse.from(plan);
    }

    public DeliveryStopResponse getDeliveryStop(
            Long planId,
            Long stopId
    ) {
        DeliveryStop stop = deliveryStopRepository.findDetailByIdAndPlanId(stopId, planId)
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_STOP_NOT_FOUND));
        log.info("{} 조회 완료 planId: {}, stopId: {}", STOP, planId, stopId);

        return DeliveryStopResponse.from(stop);
    }

    @Transactional
    public void changeScheduledDepartureAt(
            Long planId,
            UpdateScheduledDepartureRequest request
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 예정 시간 변경 요청 planId: {}, 변경 요청 시간: {}", PLAN, planId, request.scheduledDepartureAt());
        plan.updateScheduledDepartureAt(request.scheduledDepartureAt());
    }

    @Transactional
    public void reorderStops(
            Long planId,
            UpdateDeliveryOrderRequest request
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 순서 편집 요청 planId: {}", PLAN, planId);
        plan.reorderStops(request.stopIds());
    }

    @Transactional
    public void start(
            Long planId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 배송 시작 요청 planId: {}", PLAN, planId);
        plan.start();
    }

    @Transactional
    public void completeStop(
            Long planId,
            Long stopId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 포인트 배송 완료처리 요청 planId: {}, stopId: {}", PLAN, planId, stopId);
        plan.completeStop(stopId);
    }

    @Transactional
    public void completePlan(
            Long planId
    ) {
        DeliveryPlan plan = getPlanIfExists(planId);
        log.info("{} 전체 배송 완료 처리 요청 planId: {}", PLAN, planId);
        plan.finish();
    }


    private DeliveryPlan getPlanIfExists(Long planId) {
        return deliveryPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(DeliveryException.DELIVERY_PLAN_NOT_FOUND));
    }
}
