package com.example.delivery_project.exception.global;

import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void error_code_and_reason_are_mapped() {
        ErrorResponse response = ErrorResponse.of(
                DeliveryException.DELIVERY_PLAN_NOT_FOUND,
                "planId=10"
        );

        assertThat(response.getStatus())
                .isEqualTo(DeliveryException.DELIVERY_PLAN_NOT_FOUND.getStatus());
        assertThat(response.getCode()).isEqualTo("DELIVERY_PLAN_NOT_FOUND");
        assertThat(response.getReason()).isEqualTo("planId=10");
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void binding_errors_are_mapped_to_field_errors() {
        TestRequest target = new TestRequest();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "request");
        bindingResult.rejectValue("name", "NotBlank", "이름은 필수입니다");

        ErrorResponse response = ErrorResponse.of(
                ExceptionCode.INVALID_INPUT,
                bindingResult
        );

        assertThat(response.getErrors()).singleElement().satisfies(error -> {
            assertThat(error.getField()).isEqualTo("name");
            assertThat(error.getValue()).isEmpty();
            assertThat(error.getReason()).isEqualTo("이름은 필수입니다");
        });
    }

    static class TestRequest {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
