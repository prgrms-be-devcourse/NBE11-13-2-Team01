package com.example.delivery_project.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DeliveryException implements ErrorCode {
    DELIVERY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_PLAN_NOT_FOUND", "해당 배송 계획을 찾을 수 없습니다"),
    DELIVERY_STOP_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_STOP_NOT_FOUND", "해당 배송 목표지를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
