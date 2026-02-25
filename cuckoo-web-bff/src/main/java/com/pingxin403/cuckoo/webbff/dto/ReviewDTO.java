package com.pingxin403.cuckoo.webbff.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价 DTO
 */
@Data
public class ReviewDTO {
    private Long id;
    private Long productId;
    private Long userId;
    private String username;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
