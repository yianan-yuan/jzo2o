package com.jzo2o.aigc.exception;

import lombok.Getter;

@Getter
public enum AigcErrorCode {
    INVALID_REQUEST("AIGC_INVALID_REQUEST", 400, false),
    UNAUTHORIZED("AIGC_UNAUTHORIZED", 401, false),
    SESSION_NOT_FOUND("AIGC_SESSION_NOT_FOUND", 404, false),
    SESSION_EXPIRED("AIGC_SESSION_EXPIRED", 410, false),
    GENERATION_CONFLICT("AIGC_GENERATION_CONFLICT", 409, true),
    RATE_LIMITED("AIGC_RATE_LIMITED", 429, true),
    MODEL_UNAVAILABLE("AIGC_MODEL_UNAVAILABLE", 503, true),
    MODEL_OUTPUT_INVALID("AIGC_MODEL_OUTPUT_INVALID", 502, true),
    SERVICE_CATALOG_UNAVAILABLE("AIGC_SERVICE_CATALOG_UNAVAILABLE", 503, true),
    REQUEST_TIMEOUT("AIGC_REQUEST_TIMEOUT", 504, true);

    private final String code;
    private final int httpStatus;
    private final boolean retryable;

    AigcErrorCode(String code, int httpStatus, boolean retryable) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }
}
