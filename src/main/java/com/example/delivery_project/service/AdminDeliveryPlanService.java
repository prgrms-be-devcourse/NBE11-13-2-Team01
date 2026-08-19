package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.dto.response.AdminDeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.AdminDeliveryPlanSummaryResponse;
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
public class AdminDeliveryPlanService {

    private final DeliveryPlanRepository deliveryPlanRepository;

    public List<AdminDeliveryPlanSummaryResponse> getAllDeliveryPlans() {
        List<DeliveryPlan> plans =
                deliveryPlanRepository.findAllByOrderByScheduledDepartureAtAsc();

        log.info("[ADMIN][PLAN] 전체 목록 조회 완료 planSize: {}", plans.size());
        return plans.stream()
                .map(AdminDeliveryPlanSummaryResponse::from)
                .toList();
    }

    public AdminDeliveryPlanDetailResponse getDeliveryPlan(Long planId) {
        DeliveryPlan plan = deliveryPlanRepository.findDetailById(planId)
                .orElseThrow(() -> new BusinessException(
                        DeliveryException.DELIVERY_PLAN_NOT_FOUND
                ));

        log.info("[ADMIN][PLAN] 상세 조회 완료 planId: {}", planId);
        return AdminDeliveryPlanDetailResponse.from(plan);
    }
}
