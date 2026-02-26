package com.pingxin403.cuckoo.mobilebff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pingxin403.cuckoo.mobilebff.client.NotificationServiceClient;
import com.pingxin403.cuckoo.mobilebff.client.OrderServiceClient;
import com.pingxin403.cuckoo.mobilebff.client.UserServiceClient;
import com.pingxin403.cuckoo.mobilebff.dto.NotificationDTO;
import com.pingxin403.cuckoo.mobilebff.dto.OrderDTO;
import com.pingxin403.cuckoo.mobilebff.dto.UserInfoDTO;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration to provide mock beans for Mobile BFF tests
 * Mocks all Feign clients to avoid external service dependencies
 */
@TestConfiguration
public class TestConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Bean
    public UserServiceClient userServiceClient() {
        UserServiceClient client = mock(UserServiceClient.class);
        
        // Configure default behavior: return a valid user for any userId
        when(client.getUserInfo(any(Long.class))).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            UserInfoDTO user = new UserInfoDTO();
            user.setId(userId);
            user.setUsername("testuser" + userId);
            user.setNickname("Test User " + userId);
            user.setAvatar("https://example.com/avatar/" + userId + ".jpg");
            return user;
        });
        
        return client;
    }

    @Bean
    public OrderServiceClient orderServiceClient() {
        OrderServiceClient client = mock(OrderServiceClient.class);
        
        // Configure default behavior: return a list of recent orders
        when(client.getRecentOrders(any(Long.class), anyInt())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            Integer limit = invocation.getArgument(1);
            
            // Return mock orders based on limit
            List<OrderDTO> orders = Arrays.asList(
                createMockOrder(1L, "ORD-001", new BigDecimal("99.99"), "COMPLETED"),
                createMockOrder(2L, "ORD-002", new BigDecimal("149.99"), "PENDING"),
                createMockOrder(3L, "ORD-003", new BigDecimal("79.99"), "SHIPPED")
            );
            
            // Return only the requested number of orders
            return orders.subList(0, Math.min(limit, orders.size()));
        });
        
        return client;
    }

    @Bean
    public NotificationServiceClient notificationServiceClient() {
        NotificationServiceClient client = mock(NotificationServiceClient.class);
        
        // Configure default behavior: return a list of unread notifications
        when(client.getUnreadNotifications(any(Long.class))).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            
            return Arrays.asList(
                createMockNotification(1L, "订单更新", "您的订单已发货", "ORDER"),
                createMockNotification(2L, "系统通知", "欢迎使用移动端", "SYSTEM")
            );
        });
        
        return client;
    }

    /**
     * Helper method to create mock order
     */
    private OrderDTO createMockOrder(Long id, String orderNo, BigDecimal amount, String status) {
        OrderDTO order = new OrderDTO();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setTotalAmount(amount);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now().minusDays(id));
        return order;
    }

    /**
     * Helper method to create mock notification
     */
    private NotificationDTO createMockNotification(Long id, String title, String content, String type) {
        NotificationDTO notification = new NotificationDTO();
        notification.setId(id);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setCreatedAt(LocalDateTime.now().minusHours(id));
        return notification;
    }
}
