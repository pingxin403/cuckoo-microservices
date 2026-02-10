package com.pingxin403.cuckoo.notification.service;

import com.pingxin403.cuckoo.notification.dto.NotificationDTO;
import com.pingxin403.cuckoo.notification.entity.Notification;
import com.pingxin403.cuckoo.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUserId(100L);
        testNotification.setOrderId(200L);
        testNotification.setType("PAYMENT_SUCCESS");
        testNotification.setContent("您的订单 200 支付成功，支付金额：99.99 元");
        testNotification.setCreatedAt(Instant.now());
    }

    @Test
    void createNotification_ShouldCreateAndReturnNotification() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        NotificationDTO result = notificationService.createNotification(
                100L, 200L, "PAYMENT_SUCCESS",
                "您的订单 200 支付成功，支付金额：99.99 元"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getOrderId()).isEqualTo(200L);
        assertThat(result.getType()).isEqualTo("PAYMENT_SUCCESS");
        assertThat(result.getContent()).contains("订单 200 支付成功");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getNotificationsByUserId_ShouldReturnNotificationList() {
        // Given
        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUserId(100L);
        notification2.setOrderId(201L);
        notification2.setType("ORDER_CANCELLED");
        notification2.setContent("您的订单 201 已取消");
        notification2.setCreatedAt(Instant.now());

        List<Notification> notifications = Arrays.asList(testNotification, notification2);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(notifications);

        // When
        List<NotificationDTO> result = notificationService.getNotificationsByUserId(100L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo("PAYMENT_SUCCESS");
        assertThat(result.get(1).getType()).isEqualTo("ORDER_CANCELLED");

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(100L);
    }

    @Test
    void getNotificationsByUserId_WhenNoNotifications_ShouldReturnEmptyList() {
        // Given
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(999L)).thenReturn(List.of());

        // When
        List<NotificationDTO> result = notificationService.getNotificationsByUserId(999L);

        // Then
        assertThat(result).isEmpty();

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(999L);
    }
}
