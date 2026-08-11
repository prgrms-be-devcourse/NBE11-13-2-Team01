package com.example.delivery_project.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DeliveryException implements ErrorCode {
    DELIVERY_EXCEPTION(HttpStatus.NOT_FOUND, "배송을 찾을 수 없습니다.", "msg"),
    DELIVERY_NOT_FOUND(HttpStatus.BAD_REQUEST, "code", "msg"),
    DELIVERY_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "code", "msg");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
