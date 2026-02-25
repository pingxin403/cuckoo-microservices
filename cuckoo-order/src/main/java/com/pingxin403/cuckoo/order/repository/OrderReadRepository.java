package com.pingxin403.cuckoo.order.repository;

import com.pingxin403.cuckoo.order.entity.OrderRead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单读模型数据访问层
 */
@Repository
public interface OrderReadRepository extends JpaRepository<OrderRead, String> {

    /**
     * 根据用户 ID 查询订单列表（分页）
     */
    Page<OrderRead> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户 ID 查询订单列表
     */
    List<OrderRead> findByUserId(Long userId);

    /**
     * 根据状态查询订单列表
     */
    List<OrderRead> findByStatus(String status);

    /**
     * 根据用户 ID 和状态查询订单列表
     */
    List<OrderRead> findByUserIdAndStatus(Long userId, String status);
}
