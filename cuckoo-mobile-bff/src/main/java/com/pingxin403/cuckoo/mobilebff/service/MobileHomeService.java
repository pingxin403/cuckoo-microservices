package com.pingxin403.cuckoo.mobilebff.service;

import com.pingxin403.cuckoo.mobilebff.client.NotificationServiceClient;
import com.pingxin403.cuckoo.mobilebff.client.OrderServiceClient;
import com.pingxin403.cuckoo.mobilebff.client.UserServiceClient;
import com.pingxin403.cuckoo.mobilebff.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 移动端主页服务
 * 负责聚合多个后端服务的数据
 */
@Slf4j
@Service
public class MobileHomeService {

    @Autowired
    private UserServiceClient userClient;

    @Autowired
    private OrderServiceClient orderClient;

    @Autowired
    private NotificationServiceClient notificationClient;

    /**
     * 聚合主页数据
     * 并行调用多个服务以减少延迟
     */
    public CompletableFuture<HomePageResponse> aggregateHomePage(Long userId) {
        // 并行调用三个服务
        CompletableFuture<UserInfoDTO> userFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return userClient.getUserInfo(userId);
            } catch (Exception e) {
                log.error("Failed to fetch user info for userId: {}", userId, e);
                return createFallbackUser(userId);
            }
        });

        CompletableFuture<List<OrderDTO>> ordersFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return orderClient.getRecentOrders(userId, 5);
            } catch (Exception e) {
                log.error("Failed to fetch recent orders for userId: {}", userId, e);
                return Collections.emptyList();
            }
        });

        CompletableFuture<List<NotificationDTO>> notificationsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return notificationClient.getUnreadNotifications(userId);
            } catch (Exception e) {
                log.error("Failed to fetch notifications for userId: {}", userId, e);
                return Collections.emptyList();
            }
        });

        // 等待所有调用完成并聚合结果
        return CompletableFuture.allOf(userFuture, ordersFuture, notificationsFuture)
            .thenApply(v -> {
                HomePageResponse response = new HomePageResponse();
                response.setUser(userFuture.join());
                response.setRecentOrders(ordersFuture.join());
                
                List<NotificationDTO> notifications = notificationsFuture.join();
                response.setNotifications(notifications);
                response.setUnreadCount(notifications.size());
                
                log.info("Successfully aggregated home page data for user: {}", userId);
                return response;
            });
    }

    /**
     * 构建降级响应
     * 当所有服务调用失败时返回
     */
    public HomePageResponse buildDegradedResponse(Long userId) {
        log.warn("Building degraded response for user: {}", userId);
        
        HomePageResponse response = new HomePageResponse();
        response.setUser(createFallbackUser(userId));
        response.setRecentOrders(Collections.emptyList());
        response.setNotifications(Collections.emptyList());
        response.setUnreadCount(0);
        
        return response;
    }

    /**
     * 创建降级用户信息
     */
    private UserInfoDTO createFallbackUser(Long userId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(userId);
        user.setUsername("用户" + userId);
        user.setNickname("临时用户");
        return user;
    }

}
