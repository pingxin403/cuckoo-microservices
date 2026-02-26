package com.pingxin403.cuckoo.order.mapper;

import com.pingxin403.cuckoo.common.mapper.DTOMapper;
import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.entity.OrderRead;
import org.springframework.stereotype.Component;

/**
 * 订单读模型映射器（CQRS）
 * 实现 DTOMapper 接口，提供 OrderRead 实体和 OrderDTO 之间的转换
 */
@Component
public class OrderReadMapper implements DTOMapper<OrderRead, OrderDTO> {

    /**
     * 将 OrderRead 实体转换为 OrderDTO
     * 
     * @param entity OrderRead 实体对象
     * @return OrderDTO 对象，如果 entity 为 null 则返回 null
     */
    @Override
    public OrderDTO toDTO(OrderRead entity) {
        if (entity == null) {
            return null;
        }
        
        return OrderDTO.builder()
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .statusDisplay(entity.getStatusDisplay())
                .itemCount(entity.getItemCount())
                .productNames(entity.getProductNames())
                .skuIds(entity.getSkuIds())
                .paymentId(entity.getPaymentId())
                .cancelReason(entity.getCancelReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 OrderDTO 转换为 OrderRead 实体
     * 注意：读模型通常不需要从 DTO 转换回实体，因为读模型是由写模型同步生成的
     * 
     * @param dto OrderDTO 对象
     * @return OrderRead 实体对象，如果 dto 为 null 则返回 null
     */
    @Override
    public OrderRead toEntity(OrderDTO dto) {
        if (dto == null) {
            return null;
        }
        
        OrderRead entity = new OrderRead();
        entity.setOrderId(dto.getOrderId());
        entity.setUserId(dto.getUserId());
        entity.setUserName(dto.getUserName());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setStatus(dto.getStatus());
        entity.setStatusDisplay(dto.getStatusDisplay());
        entity.setItemCount(dto.getItemCount());
        entity.setProductNames(dto.getProductNames());
        entity.setSkuIds(dto.getSkuIds());
        entity.setPaymentId(dto.getPaymentId());
        entity.setCancelReason(dto.getCancelReason());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        
        return entity;
    }
    
    // toDTOList() 和 toEntityList() 方法继承自 DTOMapper 接口的默认实现
}
