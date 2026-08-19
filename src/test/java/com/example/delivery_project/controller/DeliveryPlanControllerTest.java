package com.example.delivery_project.controller;

import com.example.delivery_project.domain.entity.user.User;
import com.example.delivery_project.dto.request.UpdateDeliveryOrderRequest;
import com.example.delivery_project.dto.response.DeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.DeliveryPlanSummaryResponse;
import com.example.delivery_project.dto.response.DeliveryStopResponse;
import com.example.delivery_project.dto.response.NextStopRecommendationResponse;
import com.example.delivery_project.enums.DeliveryPlanStatus;
import com.example.delivery_project.enums.Role;
import com.example.delivery_project.security.auth.CustomUserDetails;
import com.example.delivery_project.service.DeliveryPlanService;
import com.example.delivery_project.service.NextStopRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanControllerTest {

    @Mock
    private DeliveryPlanService deliveryPlanService;

    @Mock
    private NextStopRecommendationService nextStopRecommendationService;

    @InjectMocks
    private DeliveryPlanController controller;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        User driver = User.of(
                7L,
                "driver",
                "password",
                "배송기사",
                Role.ROLE_DELIVERY_DRIVER
        );
        userDetails = new CustomUserDetails(driver);
    }

    @Test
    void 로그인_기사의_배송계획_목록을_조회한다() {
        DeliveryPlanSummaryResponse summary = new DeliveryPlanSummaryResponse(
                10L,
                "서울 물류센터",
                LocalDateTime.now().plusHours(1),
                null,
                DeliveryPlanStatus.READY,
                2,
                2,
                0
        );
        when(deliveryPlanService.getDeliveryPlans(7L))
                .thenReturn(List.of(summary));

        List<DeliveryPlanSummaryResponse> response =
                controller.getMyDeliveryPlans(userDetails);

        assertThat(response).containsExactly(summary);
        verify(deliveryPlanService).getDeliveryPlans(7L);
    }

    @Test
    void 배송계획과_배송지_상세조회_요청을_서비스에_전달한다() {
        DeliveryPlanDetailResponse planResponse = org.mockito.Mockito.mock(
                DeliveryPlanDetailResponse.class
        );
        DeliveryStopResponse stopResponse = org.mockito.Mockito.mock(
                DeliveryStopResponse.class
        );
        when(deliveryPlanService.getDeliveryPlan(10L, 7L))
                .thenReturn(planResponse);
        when(deliveryPlanService.getDeliveryStop(10L, 101L, 7L))
                .thenReturn(stopResponse);

        assertThat(controller.getDeliveryPlan(userDetails, 10L))
                .isSameAs(planResponse);
        assertThat(controller.getDeliveryStop(userDetails, 10L, 101L))
                .isSameAs(stopResponse);
    }

    @Test
    void 다음_배송지_추천_요청을_서비스에_전달한다() {
        NextStopRecommendationResponse recommendation =
                org.mockito.Mockito.mock(NextStopRecommendationResponse.class);
        when(nextStopRecommendationService.recommend(10L, 7L))
                .thenReturn(recommendation);

        assertThat(controller.recommendNextStop(userDetails, 10L))
                .isSameAs(recommendation);
        verify(nextStopRecommendationService).recommend(10L, 7L);
    }

    @Test
    void 배송순서를_변경하면_204를_반환한다() {
        UpdateDeliveryOrderRequest orderRequest =
                new UpdateDeliveryOrderRequest(List.of(102L, 101L));

        ResponseEntity<Void> orderResponse =
                controller.reorderStops(userDetails, 10L, orderRequest);

        assertThat(orderResponse.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(deliveryPlanService).reorderStops(10L, 7L, orderRequest);
    }

    @Test
    void 배송_상태변경_요청을_서비스에_전달하고_204를_반환한다() {
        ResponseEntity<Void> startResponse =
                controller.start(userDetails, 10L);
        ResponseEntity<Void> stopResponse =
                controller.completeStop(userDetails, 10L, 101L);
        ResponseEntity<Void> planResponse =
                controller.completePlan(userDetails, 10L);

        assertThat(startResponse.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(stopResponse.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(planResponse.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(deliveryPlanService).start(10L, 7L);
        verify(deliveryPlanService).completeStop(10L, 101L, 7L);
        verify(deliveryPlanService).completePlan(10L, 7L);
    }
}
