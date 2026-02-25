package com.pingxin403.cuckoo.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建订单请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @Schema(description = "用户ID", example = "1", required = true)
    private Long userId;
    
    @Schema(description = "商品SKU ID", example = "100", required = true)
    private Long skuId;
    
    @Schema(description = "购买数量", example = "2", required = true, minimum = "1")
    private Integer quantity;
}
