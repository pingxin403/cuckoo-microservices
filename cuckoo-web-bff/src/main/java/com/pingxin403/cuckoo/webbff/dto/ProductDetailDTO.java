package com.pingxin403.cuckoo.webbff.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品详情 DTO（完整版）
 */
@Data
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String brand;
    private String imageUrl;
    private String specifications;
}
