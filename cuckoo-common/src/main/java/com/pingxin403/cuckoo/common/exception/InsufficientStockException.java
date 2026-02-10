package com.pingxin403.cuckoo.common.exception;

/**
 * 库存不足异常 → HTTP 409
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(Long skuId, int requested, int available) {
        super(String.format("Insufficient stock for SKU %d: requested %d, available %d", skuId, requested, available));
    }
}
