package com.pingxin403.cuckoo.inventory.repository;

import com.pingxin403.cuckoo.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 库存数据访问层
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 根据 SKU ID 查询库存
     */
    Optional<Inventory> findBySkuId(Long skuId);

    /**
     * 根据 SKU ID 判断库存是否存在
     */
    boolean existsBySkuId(Long skuId);
}
