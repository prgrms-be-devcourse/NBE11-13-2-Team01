package com.example.delivery_project.controller;

import com.example.delivery_project.dto.request.CreateDeliveryItemRequest;
import com.example.delivery_project.dto.request.CreateDeliveryPlanRequest;
import com.example.delivery_project.dto.request.CreateDeliveryStopRequest;
import com.example.delivery_project.dto.response.AdminDeliveryPlanDetailResponse;
import com.example.delivery_project.dto.response.AdminDeliveryPlanSummaryResponse;
import com.example.delivery_project.dto.response.CreateDeliveryPlanResponse;
import com.example.delivery_project.enums.ProductType;
import com.example.delivery_project.service.AdminDeliveryPlanService;
import com.example.delivery_project.service.DeliveryPlanCreationFacade;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryPlanControllerTest {

    @Mock
    private AdminDeliveryPlanService adminDeliveryPlanService;

    @Mock
    private DeliveryPlanCreationFacade deliveryPlanCreationFacade;

    @InjectMocks
    private AdminDeliveryPlanController controller;

    @Test
    void 관리자는_전체_배송계획과_상세를_조회한다() {
        AdminDeliveryPlanSummaryResponse summary =
                mock(AdminDeliveryPlanSummaryResponse.class);
        AdminDeliveryPlanDetailResponse detail =
                mock(AdminDeliveryPlanDetailResponse.class);
        when(adminDeliveryPlanService.getAllDeliveryPlans())
                .thenReturn(List.of(summary));
        when(adminDeliveryPlanService.getDeliveryPlan(10L))
                .thenReturn(detail);

        assertThat(controller.getAllDeliveryPlans())
                .containsExactly(summary);
        assertThat(controller.getDeliveryPlan(10L)).isSameAs(detail);
    }

    @Test
    void 관리자는_특정_기사에게_배송계획을_생성해_할당한다() {
        CreateDeliveryPlanRequest request = new CreateDeliveryPlanRequest(
                "서울 물류센터",
                LocalDateTime.now().plusHours(1),
                List.of(new CreateDeliveryStopRequest(
                        "서울시청",
                        List.of(new CreateDeliveryItemRequest(
                                "냉동식품",
                                ProductType.FROZEN,
                                1
                        ))
                ))
        );
        when(deliveryPlanCreationFacade.create(7L, request))
                .thenReturn(100L);

        ResponseEntity<CreateDeliveryPlanResponse> response =
                controller.create(7L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/admin/delivery-plans/100");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().planId()).isEqualTo(100L);
        verify(deliveryPlanCreationFacade).create(7L, request);
    }
}
