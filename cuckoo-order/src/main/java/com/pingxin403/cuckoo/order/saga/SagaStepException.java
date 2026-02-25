package com.pingxin403.cuckoo.order.saga;

/**
 * Saga 步骤执行异常
 */
public class SagaStepException extends Exception {
    
    public SagaStepException(String message) {
        super(message);
    }
    
    public SagaStepException(String message, Throwable cause) {
        super(message, cause);
    }
}
