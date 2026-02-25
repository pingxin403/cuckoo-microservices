package com.pingxin403.cuckoo.webbff.dto;

import lombok.Data;

/**
 * 库存信息 DTO
 */
@Data
public class InventoryDTO {
    private Long productId;
    private Integer availableStock;
    private Integer reservedStock;
    private String status;  // AVAILABLE, LOW_STOCK, OUT_OF_STOCK, UNAVAILABLE
}
