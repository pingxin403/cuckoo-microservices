package com.pingxin403.cuckoo.notification.controller;

import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.notification.dto.NotificationDTO;
import com.pingxin403.cuckoo.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知 Controller
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    /**
     * 获取用户通知列表
     *
     * @param userId 用户 ID
     * @return 通知列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(@PathVariable Long userId) {
        logRequest("获取用户通知列表", userId);
        List<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId);
        logResponse("获取用户通知列表", notifications.size() + " 条通知");
        return ok(notifications);
    }
}
