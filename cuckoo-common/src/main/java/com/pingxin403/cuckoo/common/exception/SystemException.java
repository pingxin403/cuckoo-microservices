package com.pingxin403.cuckoo.common.exception;

/**
 * 通用系统异常 → HTTP 500
 */
public class SystemException extends RuntimeException {

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
