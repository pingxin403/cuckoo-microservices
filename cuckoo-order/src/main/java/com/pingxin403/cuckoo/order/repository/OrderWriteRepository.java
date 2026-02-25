package com.pingxin403.cuckoo.order.repository;

import com.pingxin403.cuckoo.order.entity.OrderWrite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 订单写模型数据访问层
 */
@Repository
public interface OrderWriteRepository extends JpaRepository<OrderWrite, String> {

    /**
     * 根据用户 ID 和订单 ID 查询
     */
    Optional<OrderWrite> findByOrderIdAndUserId(String orderId, Long userId);
}
