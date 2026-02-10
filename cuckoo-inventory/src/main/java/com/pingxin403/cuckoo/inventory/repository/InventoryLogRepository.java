package com.pingxin403.cuckoo.inventory.repository;

import com.pingxin403.cuckoo.inventory.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 库存操作流水日志数据访问层
 */
@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    /**
     * 根据订单 ID 查询操作流水
     */
    List<InventoryLog> findByOrderId(String orderId);

    /**
     * 根据 SKU ID 查询操作流水
     */
    List<InventoryLog> findBySkuId(Long skuId);
}
