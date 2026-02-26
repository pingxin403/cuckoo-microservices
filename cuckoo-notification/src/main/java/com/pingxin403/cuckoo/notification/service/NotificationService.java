package com.pingxin403.cuckoo.notification.service;

import com.pingxin403.cuckoo.notification.dto.NotificationDTO;
import com.pingxin403.cuckoo.notification.entity.Notification;
import com.pingxin403.cuckoo.notification.mapper.NotificationMapper;
import com.pingxin403.cuckoo.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知服务
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 创建通知
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     * @param type    通知类型
     * @param content 通知内容
     * @return 通知 DTO
     */
    @Transactional
    public NotificationDTO createNotification(Long userId, Long orderId, String type, String content) {
        Notification notification = new Notification(userId, orderId, type, content);
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification: id={}, userId={}, orderId={}, type={}",
                saved.getId(), userId, orderId, type);
        return notificationMapper.toDTO(saved);
    }

    /**
     * 根据用户 ID 查询通知列表
     *
     * @param userId 用户 ID
     * @return 通知列表
     */
    public List<NotificationDTO> getNotificationsByUserId(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notificationMapper.toDTOList(notifications);
    }
}
