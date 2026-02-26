package com.pingxin403.cuckoo.notification.mapper;

import com.pingxin403.cuckoo.common.mapper.DTOMapper;
import com.pingxin403.cuckoo.notification.dto.NotificationDTO;
import com.pingxin403.cuckoo.notification.entity.Notification;
import org.springframework.stereotype.Component;

/**
 * 通知映射器
 * 实现 DTOMapper 接口，提供 Notification 实体和 NotificationDTO 之间的转换
 */
@Component
public class NotificationMapper implements DTOMapper<Notification, NotificationDTO> {

    /**
     * 将 Notification 实体转换为 NotificationDTO
     * 
     * @param entity Notification 实体对象
     * @return NotificationDTO 对象，如果 entity 为 null 则返回 null
     */
    @Override
    public NotificationDTO toDTO(Notification entity) {
        if (entity == null) {
            return null;
        }
        
        return new NotificationDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getOrderId(),
                entity.getType(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    /**
     * 将 NotificationDTO 转换为 Notification 实体
     * 注意：不设置 createdAt，该字段由实体构造函数自动管理
     * 
     * @param dto NotificationDTO 对象
     * @return Notification 实体对象，如果 dto 为 null 则返回 null
     */
    @Override
    public Notification toEntity(NotificationDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return new Notification(
                dto.getId(),
                dto.getUserId(),
                dto.getOrderId(),
                dto.getType(),
                dto.getContent(),
                dto.getCreatedAt()
        );
    }
    
    // toDTOList() 和 toEntityList() 方法继承自 DTOMapper 接口的默认实现
}
