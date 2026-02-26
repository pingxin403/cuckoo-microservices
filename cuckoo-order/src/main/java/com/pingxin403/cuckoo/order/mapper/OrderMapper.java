package com.pingxin403.cuckoo.order.mapper;

import com.pingxin403.cuckoo.common.mapper.DTOMapper;
import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.entity.Order;
import org.springframework.stereotype.Component;

/**
 * 订单映射器
 * 实现 DTOMapper 接口，提供 Order 实体和 OrderDTO 之间的转换
 */
@Component
public class OrderMapper implements DTOMapper<Order, OrderDTO> {

    /**
     * 将 Order 实体转换为 OrderDTO
     * 
     * @param entity Order 实体对象
     * @return OrderDTO 对象，如果 entity 为 null 则返回 null
     */
    @Override
    public OrderDTO toDTO(Order entity) {
        if (entity == null) {
            return null;
        }
        
        return OrderDTO.builder()
                .id(entity.getId())
                .orderNo(entity.getOrderNo())
                .userId(entity.getUserId())
                .skuId(entity.getSkuId())
                .productName(entity.getProductName())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .cancelReason(entity.getCancelReason())
                .paymentId(entity.getPaymentId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 OrderDTO 转换为 Order 实体
     * 注意：不设置 createdAt 和 updatedAt，这些字段由 JPA 自动管理
     * 
     * @param dto OrderDTO 对象
     * @return Order 实体对象，如果 dto 为 null 则返回 null
     */
    @Override
    public Order toEntity(OrderDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Order entity = new Order();
        entity.setId(dto.getId());
        entity.setOrderNo(dto.getOrderNo());
        entity.setUserId(dto.getUserId());
        entity.setSkuId(dto.getSkuId());
        entity.setProductName(dto.getProductName());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setTotalAmount(dto.getTotalAmount());
        
        // 转换状态字符串为枚举
        if (dto.getStatus() != null) {
            try {
                entity.setStatus(Order.OrderStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                // 如果状态值无效，设置为 null
                entity.setStatus(null);
            }
        }
        
        entity.setCancelReason(dto.getCancelReason());
        entity.setPaymentId(dto.getPaymentId());
        
        return entity;
    }
    
    // toDTOList() 和 toEntityList() 方法继承自 DTOMapper 接口的默认实现
}
