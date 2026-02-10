package com.pingxin403.cuckoo.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存操作请求 DTO（预占、扣减、释放）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryOperationRequest {

    private Long skuId;
    private Integer quantity;
    private String orderId;
}
