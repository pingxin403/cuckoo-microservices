package com.pingxin403.cuckoo.order.saga;

/**
 * Saga 补偿异常
 */
public class CompensationException extends Exception {
    
    public CompensationException(String message) {
        super(message);
    }
    
    public CompensationException(String message, Throwable cause) {
        super(message, cause);
    }
}
