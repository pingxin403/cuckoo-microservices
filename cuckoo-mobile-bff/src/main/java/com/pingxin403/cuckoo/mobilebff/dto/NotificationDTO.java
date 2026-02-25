package com.pingxin403.cuckoo.mobilebff.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知 DTO（精简版）
 */
@Data
public class NotificationDTO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private LocalDateTime createdAt;
}
