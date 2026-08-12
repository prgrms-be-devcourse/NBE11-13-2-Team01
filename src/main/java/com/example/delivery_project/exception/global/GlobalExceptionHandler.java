package com.example.delivery_project.exception.global;

import com.example.delivery_project.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        final ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode(), e.getReason());

        log.warn(
                "[BUSINESS] 비즈니스 예외 발생 code: {}, message: {}, reason: {}",
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage(),
                e.getReason()
        );

        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        ErrorResponse response =
                ErrorResponse.of(ExceptionCode.INVALID_INPUT, e.getBindingResult());
        log.warn(
                "[VALIDATION] 요청 검증 실패 errorCount: {}",
                e.getBindingResult().getErrorCount()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse response =
                ErrorResponse.of(ExceptionCode.UNEXPECTED_ERROR, ExceptionCode.UNEXPECTED_ERROR.getMessage());
        log.error("[UNEXPECTED] 예상하지 못한 예외 발생", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
