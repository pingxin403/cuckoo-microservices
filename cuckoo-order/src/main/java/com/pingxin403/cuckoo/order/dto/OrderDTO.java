package com.pingxin403.cuckoo.order.dto;

import com.pingxin403.cuckoo.order.entity.Order;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "订单信息")
public class OrderDTO {

    // 传统模型字段
    @Schema(description = "订单ID（传统模型）", example = "1")
    private Long id;
    
    @Schema(description = "订单编号", example = "ORD20240225001")
    private String orderNo;
    
    // CQRS 读模型字段
    @Schema(description = "订单UUID（CQRS读模型）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String orderId;
    
    @Schema(description = "用户名称", example = "张三")
    private String userName;
    
    @Schema(description = "状态显示名称", example = "待支付")
    private String statusDisplay;
    
    @Schema(description = "订单项数量", example = "3")
    private Integer itemCount;
    
    @Schema(description = "商品名称列表", example = "iPhone 15, AirPods Pro")
    private String productNames;
    
    @Schema(description = "SKU ID列表", example = "100,101")
    private String skuIds;
    
    // 通用字段
    @Schema(description = "用户ID", example = "1")
    private Long userId;
    
    @Schema(description = "商品SKU ID", example = "100")
    private Long skuId;
    
    @Schema(description = "商品名称", example = "iPhone 15 Pro Max")
    private String productName;
    
    @Schema(description = "购买数量", example = "2")
    private Integer quantity;
    
    @Schema(description = "单价", example = "9999.00")
    private BigDecimal unitPrice;
    
    @Schema(description = "总金额", example = "19998.00")
    private BigDecimal totalAmount;
    
    @Schema(description = "订单状态", example = "PENDING", allowableValues = {"PENDING", "PAID", "CANCELLED", "COMPLETED"})
    private String status;
    
    @Schema(description = "取消原因", example = "用户主动取消")
    private String cancelReason;
    
    @Schema(description = "支付ID", example = "1001")
    private Long paymentId;
    
    @Schema(description = "创建时间", example = "2024-02-25T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间", example = "2024-02-25T11:00:00")
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
