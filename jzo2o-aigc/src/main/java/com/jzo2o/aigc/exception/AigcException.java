package com.jzo2o.aigc.exception;

import lombok.Getter;

@Getter
public class AigcException extends RuntimeException {

    private final AigcErrorCode errorCode;

    public AigcException(AigcErrorCode errorCode) {
        this(errorCode, errorCode.getCode());
    }

    public AigcException(AigcErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
