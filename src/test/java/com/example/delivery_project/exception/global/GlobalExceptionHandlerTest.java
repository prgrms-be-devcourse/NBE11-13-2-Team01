package com.example.delivery_project.exception.global;

import com.example.delivery_project.exception.DeliveryException;
import com.example.delivery_project.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void 비즈니스_예외의_상태와_사유를_응답한다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(
                        new BusinessException(
                                DeliveryException.DELIVERY_PLAN_NOT_FOUND,
                                "planId=10"
                        )
                );

        assertThat(response.getStatusCode())
                .isEqualTo(DeliveryException.DELIVERY_PLAN_NOT_FOUND.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo("DELIVERY_PLAN_NOT_FOUND");
        assertThat(response.getBody().getReason()).isEqualTo("planId=10");
    }

    @Test
    void 예상하지_못한_예외는_내부정보를_숨긴_500으로_응답한다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleException(
                        new IllegalStateException("민감한 내부 오류")
                );

        assertThat(response.getStatusCode())
                .isEqualTo(ExceptionCode.UNEXPECTED_ERROR.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo("UNEXPECTED_ERROR");
        assertThat(response.getBody().getReason())
                .doesNotContain("민감한 내부 오류");
    }

    @Test
    void 요청값_검증_오류의_필드정보를_400으로_응답한다() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError(
                "request",
                "loginId",
                "",
                false,
                null,
                null,
                "아이디는 필수입니다."
        ));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        mock(MethodParameter.class),
                        bindingResult
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleValidationException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().getErrors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.getField()).isEqualTo("loginId");
                    assertThat(error.getReason())
                            .isEqualTo("아이디는 필수입니다.");
                });
    }
}
