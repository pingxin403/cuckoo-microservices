package com.pingxin403.cuckoo.mobilebff.dto;

import lombok.Data;

import java.util.List;

/**
 * 移动端主页响应 DTO
 */
@Data
public class HomePageResponse {
    private UserInfoDTO user;
    private List<OrderDTO> recentOrders;
    private List<NotificationDTO> notifications;
    private Integer unreadCount;
}
