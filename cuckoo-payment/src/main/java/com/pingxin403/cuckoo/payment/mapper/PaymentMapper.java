package com.pingxin403.cuckoo.payment.mapper;

import com.pingxin403.cuckoo.common.mapper.DTOMapper;
import com.pingxin403.cuckoo.payment.dto.PaymentDTO;
import com.pingxin403.cuckoo.payment.entity.Payment;
import org.springframework.stereotype.Component;

/**
 * 支付映射器
 * 实现 DTOMapper 接口，提供 Payment 实体和 PaymentDTO 之间的转换
 */
@Component
public class PaymentMapper implements DTOMapper<Payment, PaymentDTO> {

    /**
     * 将 Payment 实体转换为 PaymentDTO
     * 
     * @param entity Payment 实体对象
     * @return PaymentDTO 对象，如果 entity 为 null 则返回 null
     */
    @Override
    public PaymentDTO toDTO(Payment entity) {
        if (entity == null) {
            return null;
        }
        
        return new PaymentDTO(
                entity.getId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * 将 PaymentDTO 转换为 Payment 实体
     * 注意：不设置 createdAt 和 updatedAt，这些字段由 JPA 自动管理
     * 
     * @param dto PaymentDTO 对象
     * @return Payment 实体对象，如果 dto 为 null 则返回 null
     */
    @Override
    public Payment toEntity(PaymentDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Payment entity = new Payment();
        entity.setId(dto.getId());
        entity.setOrderId(dto.getOrderId());
        entity.setUserId(dto.getUserId());
        entity.setAmount(dto.getAmount());
        
        // Convert status string to enum
        if (dto.getStatus() != null) {
            entity.setStatus(Payment.PaymentStatus.valueOf(dto.getStatus()));
        }
        
        return entity;
    }
    
    // toDTOList() 和 toEntityList() 方法继承自 DTOMapper 接口的默认实现
}
