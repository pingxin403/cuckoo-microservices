package com.pingxin403.cuckoo.webbff.dto;

import lombok.Data;

import java.util.List;

/**
 * Web 端商品详情页响应 DTO
 */
@Data
public class ProductPageResponse {
    private ProductDetailDTO product;
    private InventoryDTO inventory;
    private List<ReviewDTO> reviews;
    private Double averageRating;
    private Integer totalReviews;
}
