package com.example.delivery_project.controller;

import com.example.delivery_project.config.OpenApiConfig;
import com.example.delivery_project.dto.request.UpdateDeliveryOrderRequest;
import com.example.delivery_project.dto.response.DeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.DeliveryPlanSummaryResponse;
import com.example.delivery_project.dto.response.DeliveryStopResponse;
import com.example.delivery_project.dto.response.NextStopRecommendationResponse;
import com.example.delivery_project.security.auth.CustomUserDetails;
import com.example.delivery_project.service.DeliveryPlanService;
import com.example.delivery_project.service.NextStopRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-plans")
@PreAuthorize("hasRole('DELIVERY_DRIVER')")
@Tag(name = "배송 계획", description = "배송 기사의 배송 계획 조회 및 진행 관리 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DeliveryPlanController {

    private final DeliveryPlanService deliveryPlanService;
    private final NextStopRecommendationService nextStopRecommendationService;

    @Operation(summary = "내 배송 계획 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 계획 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "배송 기사 권한 필요")
    })
    @GetMapping
    public List<DeliveryPlanSummaryResponse> getMyDeliveryPlans(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return deliveryPlanService.getDeliveryPlans(
                userDetails.getUser().getId()
        );
    }

    @Operation(summary = "배송 계획 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 계획 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "배송 계획을 찾을 수 없음")
    })
    @GetMapping("/{planId}")
    public DeliveryPlanDetailResponse getDeliveryPlan(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "배송 계획 ID", example = "1") @PathVariable Long planId
    ) {
        return deliveryPlanService.getDeliveryPlan(
                planId,
                userDetails.getUser().getId()
        );
    }

    @Operation(summary = "배송지 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송지 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "배송 계획 또는 배송지를 찾을 수 없음")
    })
    @GetMapping("/{planId}/stops/{stopId}")
    public DeliveryStopResponse getDeliveryStop(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "배송 계획 ID", example = "1") @PathVariable Long planId,
            @Parameter(description = "배송지 ID", example = "1") @PathVariable Long stopId
    ) {
        return deliveryPlanService.getDeliveryStop(
                planId,
                stopId,
                userDetails.getUser().getId()
        );
    }

    @Operation(
            summary = "다음 배송지 추천",
            description = "배송 중 남아 있는 순서의 최대 5개 배송지 중 위험도가 가장 낮은 후보를 우선하고, 같은 위험도 후보는 좌표 기반 예상 이동시간 다익스트라 경로로 추천합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다음 배송지 추천 성공"),
            @ApiResponse(responseCode = "400", description = "배송 중인 계획이 아님"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "배송 계획을 찾을 수 없음")
    })
    @GetMapping("/{planId}/next-stop-recommendation")
    public NextStopRecommendationResponse recommendNextStop(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "배송 계획 ID", example = "1") @PathVariable Long planId
    ) {
        return nextStopRecommendationService.recommend(
                planId,
                userDetails.getUser().getId()
        );
    }

    @Operation(
            summary = "배송 순서 변경",
            description = "READY 상태인 배송 계획의 배송지 방문 순서를 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "배송 순서 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 또는 배송 계획 상태가 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "배송 계획을 찾을 수 없음")
    })
    @PutMapping("/{planId}/stops/order")
    public ResponseEntity<Void> reorderStops(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "배송 계획 ID", example = "1") @PathVariable Long planId,
            @Valid @RequestBody UpdateDeliveryOrderRequest request
    ) {
        deliveryPlanService.reorderStops(
                planId,
                userDetails.getUser().getId(),
                request
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "배송 시작", description = "READY 상태인 배송 계획을 DELIVERING 상태로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "배송 시작 성공"),
            @ApiResponse(responseCode = "400", description = "배송 계획 상태가 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "배송 계획을 찾을 수 없음")
    })
    @PostMapping("/{planId}/start")
    public ResponseEntity<Void> start(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "배송 계획 ID", example = "1") @PathVariable Long planId
    ) {
        deliveryPlanService.start(
                planId,
                userDetails.getUser().getId()
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "배송지 완료", description = "배송 중인 배송지를 COMPLETED 상태로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "배송지 완료 처리 성공"),
            @ApiResponse(responseCode = "400", description = "배송 계획 또는 배송지 상태가 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "배송 계획 또는 배송지를 찾을 수 없음")
    })
    @PostMapping("/{planId}/stops/{stopId}/complete")
    public ResponseEntity<Void> completeStop(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "배송 계획 ID", example = "1") @PathVariable Long planId,
            @Parameter(description = "배송지 ID", example = "1") @PathVariable Long stopId
    ) {
        deliveryPlanService.completeStop(
                planId,
                stopId,
                userDetails.getUser().getId()
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "배송 계획 완료",
            description = "모든 배송지가 완료된 배송 계획을 COMPLETED 상태로 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "배송 계획 완료 처리 성공"),
            @ApiResponse(responseCode = "400", description = "배송 계획 또는 배송지 상태가 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "배송 계획을 찾을 수 없음")
    })
    @PostMapping("/{planId}/complete")
    public ResponseEntity<Void> completePlan(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "배송 계획 ID", example = "1") @PathVariable Long planId
    ) {
        deliveryPlanService.completePlan(
                planId,
                userDetails.getUser().getId()
        );
        return ResponseEntity.noContent().build();
    }
}
