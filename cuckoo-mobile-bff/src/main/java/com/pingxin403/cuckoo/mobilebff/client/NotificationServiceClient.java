package com.pingxin403.cuckoo.mobilebff.client;

import com.pingxin403.cuckoo.mobilebff.dto.NotificationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 通知服务 Feign 客户端
 */
@FeignClient(
    name = "cuckoo-notification",
    path = "/api/notifications",
    fallback = NotificationServiceClientFallback.class
)
public interface NotificationServiceClient {

    /**
     * 获取未读通知
     */
    @GetMapping("/unread")
    List<NotificationDTO> getUnreadNotifications(@RequestParam("userId") Long userId);

}
