package com.example.delivery_project.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum RiskException implements ErrorCode {
    RISK_NOT_FOUND(HttpStatus.NOT_FOUND, "RISK_NOT_FOUND", "위험도가 존재하지 않습니다"),
    RISK_ARGUMENT_NOT_IMPLEMENTED(HttpStatus.BAD_REQUEST, "RISK_ARGUMENT_NOT_IMPLEMENTED", "필수 항목이 누락되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
