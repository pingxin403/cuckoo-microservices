package com.pingxin403.cuckoo.order.repository;

import com.pingxin403.cuckoo.order.entity.OrderReadSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 读模型同步状态数据访问层
 */
@Repository
public interface OrderReadSyncStatusRepository extends JpaRepository<OrderReadSyncStatus, Long> {

    /**
     * 根据事件 ID 查询同步状态
     */
    Optional<OrderReadSyncStatus> findByEventId(String eventId);

    /**
     * 根据同步状态查询列表
     */
    List<OrderReadSyncStatus> findBySyncStatus(OrderReadSyncStatus.SyncStatus syncStatus);

    /**
     * 根据订单 ID 查询同步状态列表
     */
    List<OrderReadSyncStatus> findByOrderId(String orderId);
}
