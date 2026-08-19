package com.example.delivery_project.controller;

import com.example.delivery_project.dto.request.CreateDeliveryPlanRequest;
import com.example.delivery_project.dto.request.UpdateDeliveryOrderRequest;
import com.example.delivery_project.dto.request.UpdateScheduledDepartureRequest;
import com.example.delivery_project.dto.response.CreateDeliveryPlanResponse;
import com.example.delivery_project.dto.response.DeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.DeliveryPlanSummaryResponse;
import com.example.delivery_project.dto.response.DeliveryStopResponse;
import com.example.delivery_project.security.auth.CustomUserDetails;
import com.example.delivery_project.service.DeliveryPlanCreationFacade;
import com.example.delivery_project.service.DeliveryPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-plans")
@PreAuthorize("hasRole('DELIVERY_DRIVER')")
public class DeliveryPlanController {

    private final DeliveryPlanCreationFacade deliveryPlanCreationFacade;
    private final DeliveryPlanService deliveryPlanService;

    @PostMapping
    public ResponseEntity<CreateDeliveryPlanResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateDeliveryPlanRequest request
    ) {
        Long planId = deliveryPlanCreationFacade.create(
                userDetails.getUser().getId(),
                request
        );

        return ResponseEntity
                .created(URI.create("/api/delivery-plans/" + planId))
                .body(new CreateDeliveryPlanResponse(planId));
    }

    @GetMapping
    public List<DeliveryPlanSummaryResponse> getMyDeliveryPlans(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return deliveryPlanService.getDeliveryPlans(
                userDetails.getUser().getId()
        );
    }

    @GetMapping("/{planId}")
    public DeliveryPlanDetailResponse getDeliveryPlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long planId
    ) {
        return deliveryPlanService.getDeliveryPlan(
                planId,
                userDetails.getUser().getId()
        );
    }

    @GetMapping("/{planId}/stops/{stopId}")
    public DeliveryStopResponse getDeliveryStop(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long planId,
            @PathVariable Long stopId
    ) {
        return deliveryPlanService.getDeliveryStop(
                planId,
                stopId,
                userDetails.getUser().getId()
        );
    }

    @PatchMapping("/{planId}/scheduled-departure")
    public ResponseEntity<Void> changeScheduledDepartureAt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long planId,
            @Valid @RequestBody UpdateScheduledDepartureRequest request
    ) {
        deliveryPlanService.changeScheduledDepartureAt(
                planId,
                userDetails.getUser().getId(),
                request
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{planId}/stops/order")
    public ResponseEntity<Void> reorderStops(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long planId,
            @Valid @RequestBody UpdateDeliveryOrderRequest request
    ) {
        deliveryPlanService.reorderStops(
                planId,
                userDetails.getUser().getId(),
                request
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planId}/start")
    public ResponseEntity<Void> start(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long planId
    ) {
        deliveryPlanService.start(
                planId,
                userDetails.getUser().getId()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planId}/stops/{stopId}/complete")
    public ResponseEntity<Void> completeStop(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long planId,
            @PathVariable Long stopId
    ) {
        deliveryPlanService.completeStop(
                planId,
                stopId,
                userDetails.getUser().getId()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planId}/complete")
    public ResponseEntity<Void> completePlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long planId
    ) {
        deliveryPlanService.completePlan(
                planId,
                userDetails.getUser().getId()
        );
        return ResponseEntity.noContent().build();
    }
}
