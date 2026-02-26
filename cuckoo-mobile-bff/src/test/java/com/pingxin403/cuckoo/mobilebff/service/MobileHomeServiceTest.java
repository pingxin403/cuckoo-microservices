package com.pingxin403.cuckoo.mobilebff.service;

import com.pingxin403.cuckoo.mobilebff.client.NotificationServiceClient;
import com.pingxin403.cuckoo.mobilebff.client.OrderServiceClient;
import com.pingxin403.cuckoo.mobilebff.client.UserServiceClient;
import com.pingxin403.cuckoo.mobilebff.config.TestConfig;
import com.pingxin403.cuckoo.mobilebff.dto.HomePageResponse;
import com.pingxin403.cuckoo.mobilebff.dto.NotificationDTO;
import com.pingxin403.cuckoo.mobilebff.dto.OrderDTO;
import com.pingxin403.cuckoo.mobilebff.dto.UserInfoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Test for MobileHomeService
 * Tests home page data aggregation and fallback logic
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration"
})
@ActiveProfiles("test")
@Import(TestConfig.class)
class MobileHomeServiceTest {

    @Autowired
    private MobileHomeService mobileHomeService;

    @MockBean
    private UserServiceClient userServiceClient;

    // Mock JPA-dependent beans from cuckoo-common to avoid loading them
    @MockBean
    private com.pingxin403.cuckoo.common.idempotency.IdempotencyService idempotencyService;

    @MockBean
    private com.pingxin403.cuckoo.common.audit.AuditLogService auditLogService;

    @MockBean
    private com.pingxin403.cuckoo.common.message.LocalMessageService localMessageService;

    @MockBean
    private com.pingxin403.cuckoo.common.message.MessageRetryScheduler messageRetryScheduler;

    @MockBean
    private OrderServiceClient orderServiceClient;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    @Test
    void testAggregateHomePage_Success() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 1L;
        
        UserInfoDTO mockUser = new UserInfoDTO();
        mockUser.setId(userId);
        mockUser.setUsername("testuser");
        mockUser.setNickname("Test User");
        
        OrderDTO mockOrder = new OrderDTO();
        mockOrder.setId(1L);
        mockOrder.setOrderNo("ORD-001");
        mockOrder.setTotalAmount(new BigDecimal("99.99"));
        mockOrder.setStatus("COMPLETED");
        mockOrder.setCreatedAt(LocalDateTime.now());
        
        NotificationDTO mockNotification = new NotificationDTO();
        mockNotification.setId(1L);
        mockNotification.setTitle("Test Notification");
        mockNotification.setContent("Test Content");
        mockNotification.setType("SYSTEM");
        mockNotification.setCreatedAt(LocalDateTime.now());
        
        when(userServiceClient.getUserInfo(userId)).thenReturn(mockUser);
        when(orderServiceClient.getRecentOrders(userId, 5)).thenReturn(Arrays.asList(mockOrder));
        when(notificationServiceClient.getUnreadNotifications(userId)).thenReturn(Arrays.asList(mockNotification));

        // When
        CompletableFuture<HomePageResponse> future = mobileHomeService.aggregateHomePage(userId);
        HomePageResponse response = future.get();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        
        assertThat(response.getRecentOrders()).isNotEmpty();
        assertThat(response.getRecentOrders()).hasSize(1);
        assertThat(response.getRecentOrders().get(0).getOrderNo()).isEqualTo("ORD-001");
        
        assertThat(response.getNotifications()).isNotEmpty();
        assertThat(response.getNotifications()).hasSize(1);
        assertThat(response.getUnreadCount()).isEqualTo(1);
    }

    @Test
    void testAggregateHomePage_WithMultipleOrders() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 2L;
        
        UserInfoDTO mockUser = new UserInfoDTO();
        mockUser.setId(userId);
        mockUser.setUsername("testuser2");
        
        List<OrderDTO> mockOrders = Arrays.asList(
            createOrder(1L, "ORD-001"),
            createOrder(2L, "ORD-002"),
            createOrder(3L, "ORD-003")
        );
        
        when(userServiceClient.getUserInfo(userId)).thenReturn(mockUser);
        when(orderServiceClient.getRecentOrders(userId, 5)).thenReturn(mockOrders);
        when(notificationServiceClient.getUnreadNotifications(userId)).thenReturn(Collections.emptyList());

        // When
        CompletableFuture<HomePageResponse> future = mobileHomeService.aggregateHomePage(userId);
        HomePageResponse response = future.get();

        // Then
        assertThat(response.getRecentOrders()).hasSize(3);
        assertThat(response.getUnreadCount()).isEqualTo(0);
    }

    @Test
    void testBuildDegradedResponse() {
        // Given
        Long userId = 3L;

        // When
        HomePageResponse response = mobileHomeService.buildDegradedResponse(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getUsername()).isEqualTo("用户" + userId);
        assertThat(response.getUser().getNickname()).isEqualTo("临时用户");
        
        assertThat(response.getRecentOrders()).isEmpty();
        assertThat(response.getNotifications()).isEmpty();
        assertThat(response.getUnreadCount()).isEqualTo(0);
    }

    @Test
    void testAggregateHomePage_UserServiceFails_UsesFallback() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 4L;
        
        OrderDTO mockOrder = new OrderDTO();
        mockOrder.setId(1L);
        mockOrder.setOrderNo("ORD-001");
        
        when(userServiceClient.getUserInfo(userId)).thenThrow(new RuntimeException("User service unavailable"));
        when(orderServiceClient.getRecentOrders(userId, 5)).thenReturn(Arrays.asList(mockOrder));
        when(notificationServiceClient.getUnreadNotifications(userId)).thenReturn(Collections.emptyList());

        // When
        CompletableFuture<HomePageResponse> future = mobileHomeService.aggregateHomePage(userId);
        HomePageResponse response = future.get();

        // Then - Should use fallback user
        assertThat(response).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getUsername()).isEqualTo("用户" + userId);
        assertThat(response.getUser().getNickname()).isEqualTo("临时用户");
        
        // Other services should still work
        assertThat(response.getRecentOrders()).hasSize(1);
    }

    @Test
    void testAggregateHomePage_OrderServiceFails_ReturnsEmptyList() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 5L;
        
        UserInfoDTO mockUser = new UserInfoDTO();
        mockUser.setId(userId);
        mockUser.setUsername("testuser5");
        
        when(userServiceClient.getUserInfo(userId)).thenReturn(mockUser);
        when(orderServiceClient.getRecentOrders(userId, 5)).thenThrow(new RuntimeException("Order service unavailable"));
        when(notificationServiceClient.getUnreadNotifications(userId)).thenReturn(Collections.emptyList());

        // When
        CompletableFuture<HomePageResponse> future = mobileHomeService.aggregateHomePage(userId);
        HomePageResponse response = future.get();

        // Then - Should return empty order list
        assertThat(response).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getRecentOrders()).isEmpty();
        assertThat(response.getNotifications()).isEmpty();
    }

    @Test
    void testAggregateHomePage_NotificationServiceFails_ReturnsEmptyList() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 6L;
        
        UserInfoDTO mockUser = new UserInfoDTO();
        mockUser.setId(userId);
        mockUser.setUsername("testuser6");
        
        when(userServiceClient.getUserInfo(userId)).thenReturn(mockUser);
        when(orderServiceClient.getRecentOrders(userId, 5)).thenReturn(Collections.emptyList());
        when(notificationServiceClient.getUnreadNotifications(userId)).thenThrow(new RuntimeException("Notification service unavailable"));

        // When
        CompletableFuture<HomePageResponse> future = mobileHomeService.aggregateHomePage(userId);
        HomePageResponse response = future.get();

        // Then - Should return empty notification list
        assertThat(response).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getNotifications()).isEmpty();
        assertThat(response.getUnreadCount()).isEqualTo(0);
    }

    @Test
    void testAggregateHomePage_AllServicesFail_UsesFallbackForAll() throws ExecutionException, InterruptedException {
        // Given
        Long userId = 7L;
        
        when(userServiceClient.getUserInfo(userId)).thenThrow(new RuntimeException("User service unavailable"));
        when(orderServiceClient.getRecentOrders(userId, 5)).thenThrow(new RuntimeException("Order service unavailable"));
        when(notificationServiceClient.getUnreadNotifications(userId)).thenThrow(new RuntimeException("Notification service unavailable"));

        // When
        CompletableFuture<HomePageResponse> future = mobileHomeService.aggregateHomePage(userId);
        HomePageResponse response = future.get();

        // Then - Should use fallback for all services
        assertThat(response).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getUsername()).isEqualTo("用户" + userId);
        assertThat(response.getRecentOrders()).isEmpty();
        assertThat(response.getNotifications()).isEmpty();
        assertThat(response.getUnreadCount()).isEqualTo(0);
    }

    private OrderDTO createOrder(Long id, String orderNo) {
        OrderDTO order = new OrderDTO();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus("COMPLETED");
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
