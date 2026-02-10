package com.pingxin403.cuckoo.order.repository;

import com.pingxin403.cuckoo.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单数据访问层
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 根据订单编号查询订单
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 根据用户 ID 查询订单列表
     */
    List<Order> findByUserId(Long userId);

    /**
     * 查询超时未支付的订单
     * @param status 订单状态
     * @param timeoutBefore 超时时间点
     */
    List<Order> findByStatusAndCreatedAtBefore(Order.OrderStatus status, LocalDateTime timeoutBefore);
}
