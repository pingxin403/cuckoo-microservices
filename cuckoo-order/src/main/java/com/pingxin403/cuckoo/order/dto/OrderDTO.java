package com.pingxin403.cuckoo.order.dto;

import com.pingxin403.cuckoo.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 DTO
 * 支持传统模型和 CQRS 读模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {

    // 传统模型字段
    private Long id;
    private String orderNo;
    
    // CQRS 读模型字段
    private String orderId;
    private String userName;
    private String statusDisplay;
    private Integer itemCount;
    private String productNames;
    private String skuIds;
    
    // 通用字段
    private Long userId;
    private Long skuId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String status;
    private String cancelReason;
    private Long paymentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为 DTO
     */
    public static OrderDTO fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        return OrderDTO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .skuId(order.getSkuId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .cancelReason(order.getCancelReason())
                .paymentId(order.getPaymentId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
