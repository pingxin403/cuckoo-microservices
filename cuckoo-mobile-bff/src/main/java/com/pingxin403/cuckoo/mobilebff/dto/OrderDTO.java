package com.pingxin403.cuckoo.mobilebff.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 DTO（精简版）
 */
@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
}
