package com.pingxin403.cuckoo.notification.service;

import com.pingxin403.cuckoo.notification.TestNotificationApplication;
import com.pingxin403.cuckoo.notification.config.TestConfig;
import com.pingxin403.cuckoo.notification.dto.NotificationDTO;
import com.pingxin403.cuckoo.notification.repository.NotificationRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Positive;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for NotificationService
 *
 * Tests notification creation, content correctness, and query behavior.
 *
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 */
@JqwikSpringSupport
@SpringBootTest(classes = TestNotificationApplication.class)
@ActiveProfiles("test")
@Import(TestConfig.class)
class NotificationServicePropertyTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    // ========== Notification Type Constants ==========

    private static final String[] NOTIFICATION_TYPES = {
            "PAYMENT_SUCCESS", "ORDER_CANCELLED"
    };

    // ========== Property Tests ==========

    /**
     * Property: Notification creation persists all fields correctly
     *
     * For any valid notification data, creating a notification should persist
     * userId, orderId, type, and content correctly and assign an ID.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Notification creation persists all fields correctly")
    @Transactional
    void notificationCreation_persistsAllFieldsCorrectly(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long orderId,
            @ForAll("notificationTypes") String type,
            @ForAll("notificationContents") String content) {

        notificationRepository.deleteAll();

        NotificationDTO created = notificationService.createNotification(userId, orderId, type, content);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getOrderId()).isEqualTo(orderId);
        assertThat(created.getType()).isEqualTo(type);
        assertThat(created.getContent()).isEqualTo(content);
        assertThat(created.getCreatedAt()).isNotNull();
    }

    /**
     * Property: Notification query by userId returns correct notifications
     *
     * For any created notification, querying by userId should return
     * that notification in the results.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Notification query by userId returns correct notifications")
    @Transactional
    void notificationQuery_byUserId_returnsCorrectNotifications(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long orderId,
            @ForAll("notificationTypes") String type,
            @ForAll("notificationContents") String content) {

        notificationRepository.deleteAll();

        NotificationDTO created = notificationService.createNotification(userId, orderId, type, content);

        List<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId);

        assertThat(notifications).isNotEmpty();
        assertThat(notifications).anyMatch(n ->
                n.getId().equals(created.getId())
                        && n.getUserId().equals(userId)
                        && n.getOrderId().equals(orderId)
                        && n.getType().equals(type)
                        && n.getContent().equals(content)
        );
    }

    /**
     * Property: Multiple notifications for same user are all returned
     *
     * For any user with multiple notifications, querying should return all of them.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Multiple notifications for same user are all returned")
    @Transactional
    void multipleNotifications_forSameUser_allReturned(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long orderId1,
            @ForAll @Positive Long orderId2) {

        notificationRepository.deleteAll();

        notificationService.createNotification(userId, orderId1, "PAYMENT_SUCCESS", "Payment done for order " + orderId1);
        notificationService.createNotification(userId, orderId2, "ORDER_CANCELLED", "Order " + orderId2 + " cancelled");

        List<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId);

        assertThat(notifications).hasSize(2);
    }

    /**
     * Property: Notifications for different users are isolated
     *
     * For any two different users, querying notifications for one user
     * should not return notifications belonging to the other.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Notifications for different users are isolated")
    @Transactional
    void notifications_forDifferentUsers_areIsolated(
            @ForAll @Positive Long userId1,
            @ForAll @Positive Long userId2,
            @ForAll @Positive Long orderId) {

        Assume.that(!userId1.equals(userId2));

        notificationRepository.deleteAll();

        notificationService.createNotification(userId1, orderId, "PAYMENT_SUCCESS", "Payment success");
        notificationService.createNotification(userId2, orderId, "ORDER_CANCELLED", "Order cancelled");

        List<NotificationDTO> user1Notifications = notificationService.getNotificationsByUserId(userId1);
        List<NotificationDTO> user2Notifications = notificationService.getNotificationsByUserId(userId2);

        assertThat(user1Notifications).hasSize(1);
        assertThat(user1Notifications.get(0).getUserId()).isEqualTo(userId1);

        assertThat(user2Notifications).hasSize(1);
        assertThat(user2Notifications.get(0).getUserId()).isEqualTo(userId2);
    }

    /**
     * Property: Notification content is preserved exactly as provided
     *
     * For any content string, the notification should store and return
     * the exact same content without modification.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Notification content is preserved exactly as provided")
    @Transactional
    void notificationContent_isPreservedExactly(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long orderId,
            @ForAll("notificationContents") String content) {

        notificationRepository.deleteAll();

        NotificationDTO created = notificationService.createNotification(
                userId, orderId, "PAYMENT_SUCCESS", content);

        List<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId);

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getContent()).isEqualTo(content);
        assertThat(notifications.get(0).getContent()).isEqualTo(created.getContent());
    }

    /**
     * Property: Query for user with no notifications returns empty list
     *
     * For any userId that has no notifications, the query should return
     * an empty list (not null).
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Query for user with no notifications returns empty list")
    @Transactional
    void queryForUserWithNoNotifications_returnsEmptyList(
            @ForAll @Positive Long userId) {

        notificationRepository.deleteAll();

        List<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId);

        assertThat(notifications).isNotNull();
        assertThat(notifications).isEmpty();
    }

    // ========== Data Generators ==========

    /**
     * Generate notification types
     */
    @Provide
    Arbitrary<String> notificationTypes() {
        return Arbitraries.of(NOTIFICATION_TYPES);
    }

    /**
     * Generate notification content strings
     */
    @Provide
    Arbitrary<String> notificationContents() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(100);
    }
}
