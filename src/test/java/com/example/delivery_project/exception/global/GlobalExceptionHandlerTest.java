package com.example.delivery_project.exception.global;

import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void business_exception_uses_domain_status_and_reason() {
        var response = handler.handleBusinessException(
                new BusinessException(
                        DeliveryException.DELIVERY_PLAN_NOT_FOUND,
                        "planId=10"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("DELIVERY_PLAN_NOT_FOUND");
        assertThat(response.getBody().getReason()).isEqualTo("planId=10");
    }

    @Test
    void unexpected_exception_hides_internal_details() {
        var response = handler.handleException(new RuntimeException("database password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ExceptionCode.UNEXPECTED_ERROR.getCode());
        assertThat(response.getBody().getReason())
                .isEqualTo(ExceptionCode.UNEXPECTED_ERROR.getMessage());
    }
}
