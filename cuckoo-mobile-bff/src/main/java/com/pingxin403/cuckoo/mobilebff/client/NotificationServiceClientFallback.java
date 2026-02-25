package com.pingxin403.cuckoo.mobilebff.client;

import com.pingxin403.cuckoo.mobilebff.dto.NotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 通知服务降级处理
 */
@Slf4j
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        log.warn("Notification service fallback triggered for userId: {}", userId);
        return Collections.emptyList();
    }

}
