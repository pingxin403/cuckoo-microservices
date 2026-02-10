package com.pingxin403.cuckoo.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建商品请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    private String name;
    private BigDecimal price;
    private String description;
}
