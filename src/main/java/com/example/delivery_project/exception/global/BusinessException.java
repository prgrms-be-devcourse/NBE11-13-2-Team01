package com.example.delivery_project.exception.global;

import com.example.delivery_project.exception.ErrorCode;
import lombok.Getter;


@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String reason;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, String reason) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.reason = reason;
    }
}