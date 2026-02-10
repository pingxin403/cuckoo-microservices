package com.pingxin403.cuckoo.notification.repository;

import com.pingxin403.cuckoo.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 根据用户 ID 查询通知列表
     *
     * @param userId 用户 ID
     * @return 通知列表
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
