package com.pingxin403.cuckoo.common.exception;

/**
 * 认证异常 → HTTP 401
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
