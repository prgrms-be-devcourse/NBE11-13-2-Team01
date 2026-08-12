package com.example.delivery_project.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DeliveryException implements ErrorCode {
    DELIVERY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_PLAN_NOT_FOUND", "해당 배송 계획을 찾을 수 없습니다"),
    DELIVERY_STOP_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_STOP_NOT_FOUND", "해당 배송 목표지를 찾을 수 없습니다"),
    DELIVERY_PLAN_NOT_READY_TO_START(HttpStatus.BAD_REQUEST, "DELIVERY_PLAN_NOT_READY_TO_START", "배송 시작을 할 수 없는 상태입니다"),
    DELIVERY_INVALID_PLAN_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "DELIVERY_INVALID_PLAN_STATUS_CHANGE", "배송 상태를 변경할 수 없는 상태입니다"),
    DELIVERY_INCOMPLETE_STOP(HttpStatus.I_AM_A_TEAPOT, "DELIVERY_INCOMPLETE_STOP", "배송 완료로 전환할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
