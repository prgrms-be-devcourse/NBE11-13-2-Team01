package com.example.delivery_project.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode implements ErrorCode {

    EXAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "code", "msg"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 올바르지 않습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;

}
