package com.pingxin403.cuckoo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预占库存请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveInventoryRequest {

    private Long skuId;
    private Integer quantity;
    private String orderId;
}
