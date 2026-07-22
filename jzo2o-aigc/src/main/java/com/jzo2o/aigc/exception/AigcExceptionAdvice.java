package com.jzo2o.aigc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.jzo2o.mvc.constants.HeaderConstants.BODY_PROCESSED;

@RestControllerAdvice
public class AigcExceptionAdvice {

    @ExceptionHandler(AigcException.class)
    public ResponseEntity<Map<String, Object>> handle(AigcException exception) {
        AigcErrorCode errorCode = exception.getErrorCode();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", errorCode.getCode());
        body.put("message", exception.getMessage());
        body.put("retryable", errorCode.isRetryable());
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus()))
                .header(BODY_PROCESSED, "1")
                .body(body);
    }
}
