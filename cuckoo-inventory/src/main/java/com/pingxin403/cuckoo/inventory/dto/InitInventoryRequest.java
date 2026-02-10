package com.pingxin403.cuckoo.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 初始化库存请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitInventoryRequest {

    private Long skuId;
    private Integer totalStock;
}
