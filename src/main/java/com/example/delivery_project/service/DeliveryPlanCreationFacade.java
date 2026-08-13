package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.request.CreateDeliveryItemRequest;
import com.example.delivery_project.dto.request.CreateDeliveryPlanRequest;
import com.example.delivery_project.dto.request.CreateDeliveryStopRequest;
import com.example.delivery_project.dto.response.WeatherRiskResponse;
import com.example.delivery_project.exception.ExceptionCode;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.spec.DeliveryItemSpec;
import com.example.delivery_project.spec.DeliveryStopSpec;
import com.example.delivery_project.spec.Location;
import com.example.delivery_project.spec.RiskFactorSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryPlanCreationFacade {

    private final UserRepository userRepository;
    private final DeliveryPlanRepository deliveryPlanRepository;

    public Long create(
            Long driverId,
            CreateDeliveryPlanRequest request,
            WeatherRiskResponse weatherRisk
    ) {
        User driver = getDriver(driverId);

        List<RiskFactorSpec> riskFactorSpecs =
                toRiskFactorSpecs(weatherRisk);

        List<DeliveryStopSpec> stopSpecs =
                request.stops().stream()
                        .map(stopRequest ->
                                toStopSpec(
                                        stopRequest,
                                        riskFactorSpecs
                                )
                        )
                        .toList();

        DeliveryPlan plan = DeliveryPlanFactory.create(
                driver,
                request.toDepartureLocation(),
                request.scheduledDepartureAt(),
                stopSpecs
        );

        DeliveryPlan savedPlan =
                deliveryPlanRepository.save(plan);

        log.info(
                "[PLAN] 생성 완료 planId: {}, driverId: {}",
                savedPlan.getId(),
                driverId
        );

        return savedPlan.getId();
    }

    private DeliveryStopSpec toStopSpec(
            CreateDeliveryStopRequest request,
            List<RiskFactorSpec> riskFactorSpecs
    ) {
        List<DeliveryItemSpec> itemSpecs =
                request.items().stream()
                        .map(this::toItemSpec)
                        .toList();

        Location location = new Location(
                request.address(),
                request.latitude(),
                request.longitude()
        );

        return new DeliveryStopSpec(
                location,
                itemSpecs,
                riskFactorSpecs
        );
    }

    private DeliveryItemSpec toItemSpec(
            CreateDeliveryItemRequest request
    ) {
        return new DeliveryItemSpec(
                request.productName(),
                request.productType(),
                request.quantity()
        );
    }

    private List<RiskFactorSpec> toRiskFactorSpecs(
            WeatherRiskResponse response
    ) {
        return response.factors().stream()
                .map(factor -> new RiskFactorSpec(
                        factor.type(),
                        factor.description()
                ))
                .toList();
    }

    private User getDriver(Long driverId) {
        return userRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(
                        ExceptionCode.INVALID_INPUT,
                        "존재하지 않는 driverId입니다: " + driverId
                ));
    }
}