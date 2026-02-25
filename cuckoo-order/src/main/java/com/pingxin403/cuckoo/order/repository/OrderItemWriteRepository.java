package com.pingxin403.cuckoo.order.repository;

import com.pingxin403.cuckoo.order.entity.OrderItemWrite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单明细写模型数据访问层
 */
@Repository
public interface OrderItemWriteRepository extends JpaRepository<OrderItemWrite, Long> {

    /**
     * 根据订单 ID 查询订单明细列表
     */
    List<OrderItemWrite> findByOrderId(String orderId);
}
