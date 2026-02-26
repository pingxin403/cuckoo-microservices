package com.pingxin403.cuckoo.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
