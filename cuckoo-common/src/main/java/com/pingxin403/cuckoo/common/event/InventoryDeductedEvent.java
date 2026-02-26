package com.pingxin403.cuckoo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 库存扣减事件
 * 发布者：Inventory_Service
 * 消费者：Order_Service, Notification_Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryDeductedEvent extends DomainEvent {

    private String orderId;
    private List<InventoryChange> changes;

    public InventoryDeductedEvent(String orderId, List<InventoryChange> changes, Integer version) {
        init("INVENTORY_DEDUCTED", version);
        this.orderId = orderId;
        this.changes = changes;
    }

    /**
     * 使用默认版本号 1 创建事件
     */
    public static InventoryDeductedEvent create(String orderId, List<InventoryChange> changes) {
        return new InventoryDeductedEvent(orderId, changes, 1);
    }

    /**
     * 库存变更记录
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryChange {
        private Long skuId;
        private Integer quantity;
        private Integer beforeQuantity;
        private Integer afterQuantity;
    }
}
