package com.pingxin403.cuckoo.common.exception;

/**
 * 通用业务异常 → HTTP 400
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
