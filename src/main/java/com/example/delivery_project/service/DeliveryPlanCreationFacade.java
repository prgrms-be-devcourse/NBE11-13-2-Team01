package com.example.delivery_project.service;

import com.example.delivery_project.domain.entity.delivery.DeliveryPlan;
import com.example.delivery_project.domain.entity.delivery.DeliveryPlanFactory;
import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.UserRepository;
import com.example.delivery_project.dto.request.CreateDeliveryItemRequest;
import com.example.delivery_project.dto.request.CreateDeliveryPlanRequest;
import com.example.delivery_project.dto.request.CreateDeliveryStopRequest;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.event.DeliveryPlanCreatedEvent;
import com.example.delivery_project.exception.ExceptionCode;
import com.example.delivery_project.exception.global.BusinessException;
import com.example.delivery_project.service.component.GeocodingClient;
import com.example.delivery_project.service.component.LocationMapper;
import com.example.delivery_project.spec.DeliveryItemSpec;
import com.example.delivery_project.spec.DeliveryStopSpec;
import com.example.delivery_project.spec.GeocodedLocation;
import com.example.delivery_project.spec.Location;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final GeocodingClient geocodingClient;
    private final LocationMapper locationMapper;
    private final ApplicationEventPublisher eventPublisher;

    public Long create(
            Long driverId,
            CreateDeliveryPlanRequest request
    ) {
        User driver = getDriver(driverId);

        Location departureLocation = resolveLocation(
                request.departureAddress()
        );

        List<DeliveryStopSpec> stopSpecs = request.stops().stream()
                .map(this::toStopSpec)
                .toList();

        DeliveryPlan plan = DeliveryPlanFactory.create(
                driver,
                departureLocation,
                request.scheduledDepartureAt(),
                stopSpecs
        );

        DeliveryPlan savedPlan = deliveryPlanRepository.save(plan);

        log.info(
                "[PLAN] 생성 완료 planId: {}, driverId: {}",
                savedPlan.getId(),
                driverId
        );

        eventPublisher.publishEvent(
                new DeliveryPlanCreatedEvent(savedPlan.getId())
        );

        return savedPlan.getId();
    }

    private DeliveryStopSpec toStopSpec(
            CreateDeliveryStopRequest request
    ) {
        Location location = resolveLocation(request.address());

        List<DeliveryItemSpec> itemSpecs = request.items().stream()
                .map(this::toItemSpec)
                .toList();

        return new DeliveryStopSpec(
                location,
                itemSpecs,
                List.of()
        );
    }

    private Location resolveLocation(String address) {
        GeocodedLocation result =
                geocodingClient.geocode(address);

        return locationMapper.toLocation(result);
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

    private User getDriver(Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(
                        ExceptionCode.INVALID_INPUT,
                        "존재하지 않는 driverId입니다: " + driverId
                ));

        if (driver.getRole() != Role.ROLE_DELIVERY_DRIVER) {
            throw new BusinessException(
                    ExceptionCode.INVALID_INPUT,
                    "배송 기사에게만 계획을 할당할 수 있습니다: " + driverId
            );
        }

        return driver;
    }
}
