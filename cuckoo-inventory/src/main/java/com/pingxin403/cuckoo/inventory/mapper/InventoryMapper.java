package com.pingxin403.cuckoo.inventory.mapper;

import com.pingxin403.cuckoo.common.mapper.DTOMapper;
import com.pingxin403.cuckoo.inventory.dto.InventoryDTO;
import com.pingxin403.cuckoo.inventory.entity.Inventory;
import org.springframework.stereotype.Component;

/**
 * 库存映射器
 * 实现 DTOMapper 接口，提供 Inventory 实体和 InventoryDTO 之间的转换
 */
@Component
public class InventoryMapper implements DTOMapper<Inventory, InventoryDTO> {

    /**
     * 将 Inventory 实体转换为 InventoryDTO
     * 
     * @param entity Inventory 实体对象
     * @return InventoryDTO 对象，如果 entity 为 null 则返回 null
     */
    @Override
    public InventoryDTO toDTO(Inventory entity) {
        if (entity == null) {
            return null;
        }
        
        return InventoryDTO.builder()
                .id(entity.getId())
                .skuId(entity.getSkuId())
                .totalStock(entity.getTotalStock())
                .availableStock(entity.getAvailableStock())
                .reservedStock(entity.getReservedStock())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 InventoryDTO 转换为 Inventory 实体
     * 注意：不设置 createdAt 和 updatedAt，这些字段由 JPA 自动管理
     * 
     * @param dto InventoryDTO 对象
     * @return Inventory 实体对象，如果 dto 为 null 则返回 null
     */
    @Override
    public Inventory toEntity(InventoryDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return Inventory.builder()
                .id(dto.getId())
                .skuId(dto.getSkuId())
                .totalStock(dto.getTotalStock())
                .availableStock(dto.getAvailableStock())
                .reservedStock(dto.getReservedStock())
                .build();
    }
    
    // toDTOList() 和 toEntityList() 方法继承自 DTOMapper 接口的默认实现
}
