package com.pingxin403.cuckoo.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 库存信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDTO {

    private Long id;
    private Long skuId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer reservedStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
