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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DeliveryPlanService {
    private final DeliveryPlanRepository deliveryPlanRepository;
    private final DeliveryStopRepository deliveryStopRepository;

    public List<DeliveryPlanSummaryResponse> getDeliveryPlans(Long driverId) {
        List<DeliveryPlan> deliveryPlans = deliveryPlanRepository.findAllByDriverId(driverId);

        return deliveryPlans.stream()
                .map(DeliveryPlanSummaryResponse::from)
                .toList();
    }

    public DeliveryPlanDetailResponse getDeliveryPlan(Long planId) {
        // TODO 커스텀 예외로 변경
        DeliveryPlan plan = deliveryPlanRepository.findDetailById(planId)
                .orElseThrow(IllegalStateException::new);
        return DeliveryPlanDetailResponse.from(plan);
    }

    public DeliveryStopResponse getDeliveryStop(
            Long planId,
            Long stopId
    ) {
        // TODO 커스텀 예외로 변경
        DeliveryStop stop = deliveryStopRepository.findDetailByIdAndPlanId(stopId, planId)
                .orElseThrow(IllegalStateException::new);

        return DeliveryStopResponse.from(stop);
    }

    @Transactional
    public void changeScheduledDepartureAt(
            Long planId,
            UpdateScheduledDepartureRequest request
    ) {
        // TODO 커스텀 예외로 변경
        DeliveryPlan plan = deliveryPlanRepository.findById(planId)
                .orElseThrow(IllegalStateException::new);
        plan.updateScheduledDepartureAt(
                request.scheduledDepartureAt()
        );
    }

    @Transactional
    public void reorderStops(
            Long planId,
            UpdateDeliveryOrderRequest request
    ) {
        // TODO 커스텀 예외로 변경
        DeliveryPlan plan = deliveryPlanRepository.findById(planId)
                .orElseThrow(IllegalStateException::new);
        plan.reorderStops(request.stopIds());
    }

    @Transactional
    public void start(
            Long planId
    ) {
        // TODO 커스텀 예외로 변경
        DeliveryPlan plan = deliveryPlanRepository.findById(planId)
                .orElseThrow(IllegalStateException::new);
        plan.start();
    }

    @Transactional
    public void completeStop(
            Long planId,
            Long stopId
    ) {
        // TODO 커스텀 예외로 변경
        DeliveryPlan plan = deliveryPlanRepository.findById(planId)
                .orElseThrow(IllegalStateException::new);
        plan.completeStop(stopId);
    }

    @Transactional
    public void completePlan(
            Long planId
    ) {
        // TODO 커스텀 예외로 변경
        DeliveryPlan plan = deliveryPlanRepository.findById(planId)
                .orElseThrow(IllegalStateException::new);
        plan.finish();
    }
}
