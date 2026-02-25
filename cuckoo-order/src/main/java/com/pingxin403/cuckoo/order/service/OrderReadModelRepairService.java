package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.order.entity.OrderRead;
import com.pingxin403.cuckoo.order.entity.OrderWrite;
import com.pingxin403.cuckoo.order.repository.OrderReadRepository;
import com.pingxin403.cuckoo.order.repository.OrderWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单读模型数据修复服务
 * 提供手动同步和数据一致性检查工具
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReadModelRepairService {

    private final OrderWriteRepository orderWriteRepository;
    private final OrderReadRepository orderReadRepository;
    private final OrderReadModelUpdater orderReadModelUpdater;

    /**
     * 手动同步单个订单的读模型
     */
    @Transactional
    public void syncSingleOrder(String orderId) {
        log.info("手动同步单个订单读模型: orderId={}", orderId);
        orderReadModelUpdater.syncOrderReadModel(orderId);
        log.info("单个订单读模型同步完成: orderId={}", orderId);
    }

    /**
     * 批量同步所有订单的读模型
     */
    @Transactional
    public void syncAllOrders() {
        log.info("开始批量同步所有订单读模型");

        List<OrderWrite> allOrders = orderWriteRepository.findAll();
        log.info("共找到 {} 个订单需要同步", allOrders.size());

        int successCount = 0;
        int failCount = 0;

        for (OrderWrite orderWrite : allOrders) {
            try {
                orderReadModelUpdater.syncOrderReadModel(orderWrite.getOrderId());
                successCount++;
            } catch (Exception e) {
                log.error("同步订单读模型失败: orderId={}", orderWrite.getOrderId(), e);
                failCount++;
            }
        }

        log.info("批量同步完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
    }

    /**
     * 检查读写模型数据一致性
     * 返回不一致的订单列表
     */
    @Transactional(readOnly = true)
    public List<String> checkDataConsistency() {
        log.info("开始检查读写模型数据一致性");

        List<String> inconsistentOrders = new ArrayList<>();

        // 1. 检查写模型中存在但读模型中不存在的订单
        List<OrderWrite> allWriteOrders = orderWriteRepository.findAll();
        Map<String, OrderRead> readOrderMap = orderReadRepository.findAll()
                .stream()
                .collect(Collectors.toMap(OrderRead::getOrderId, o -> o));

        for (OrderWrite writeOrder : allWriteOrders) {
            String orderId = writeOrder.getOrderId();
            OrderRead readOrder = readOrderMap.get(orderId);

            if (readOrder == null) {
                log.warn("订单在读模型中不存在: orderId={}", orderId);
                inconsistentOrders.add(orderId + " (读模型缺失)");
                continue;
            }

            // 2. 检查关键字段是否一致
            if (!writeOrder.getUserId().equals(readOrder.getUserId())) {
                log.warn("订单用户 ID 不一致: orderId={}, write={}, read={}",
                        orderId, writeOrder.getUserId(), readOrder.getUserId());
                inconsistentOrders.add(orderId + " (用户 ID 不一致)");
            }

            if (!writeOrder.getTotalAmount().equals(readOrder.getTotalAmount())) {
                log.warn("订单总金额不一致: orderId={}, write={}, read={}",
                        orderId, writeOrder.getTotalAmount(), readOrder.getTotalAmount());
                inconsistentOrders.add(orderId + " (总金额不一致)");
            }

            if (!writeOrder.getStatus().name().equals(readOrder.getStatus())) {
                log.warn("订单状态不一致: orderId={}, write={}, read={}",
                        orderId, writeOrder.getStatus(), readOrder.getStatus());
                inconsistentOrders.add(orderId + " (状态不一致)");
            }
        }

        // 3. 检查读模型中存在但写模型中不存在的订单（孤儿数据）
        List<OrderRead> allReadOrders = orderReadRepository.findAll();
        Map<String, OrderWrite> writeOrderMap = allWriteOrders.stream()
                .collect(Collectors.toMap(OrderWrite::getOrderId, o -> o));

        for (OrderRead readOrder : allReadOrders) {
            String orderId = readOrder.getOrderId();
            if (!writeOrderMap.containsKey(orderId)) {
                log.warn("订单在写模型中不存在（孤儿数据）: orderId={}", orderId);
                inconsistentOrders.add(orderId + " (写模型缺失，孤儿数据)");
            }
        }

        log.info("数据一致性检查完成: 发现 {} 个不一致的订单", inconsistentOrders.size());
        return inconsistentOrders;
    }

    /**
     * 修复不一致的数据
     * 根据写模型重新同步读模型
     */
    @Transactional
    public void repairInconsistentData() {
        log.info("开始修复不一致的数据");

        List<String> inconsistentOrders = checkDataConsistency();
        if (inconsistentOrders.isEmpty()) {
            log.info("没有发现不一致的数据");
            return;
        }

        log.info("发现 {} 个不一致的订单，开始修复", inconsistentOrders.size());

        int successCount = 0;
        int failCount = 0;

        for (String inconsistentOrder : inconsistentOrders) {
            // 提取订单 ID（去掉后缀说明）
            String orderId = inconsistentOrder.split(" ")[0];

            try {
                // 检查是否是孤儿数据
                if (inconsistentOrder.contains("写模型缺失")) {
                    // 删除孤儿数据
                    orderReadRepository.deleteById(orderId);
                    log.info("已删除孤儿数据: orderId={}", orderId);
                } else {
                    // 重新同步读模型
                    orderReadModelUpdater.syncOrderReadModel(orderId);
                    log.info("已修复订单读模型: orderId={}", orderId);
                }
                successCount++;
            } catch (Exception e) {
                log.error("修复订单读模型失败: orderId={}", orderId, e);
                failCount++;
            }
        }

        log.info("数据修复完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
    }

    /**
     * 获取数据一致性报告
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConsistencyReport() {
        log.info("生成数据一致性报告");

        long writeCount = orderWriteRepository.count();
        long readCount = orderReadRepository.count();
        List<String> inconsistentOrders = checkDataConsistency();

        Map<String, Object> report = Map.of(
                "writeModelCount", writeCount,
                "readModelCount", readCount,
                "inconsistentCount", inconsistentOrders.size(),
                "inconsistentOrders", inconsistentOrders,
                "consistencyRate", writeCount > 0 ? 
                        (double) (writeCount - inconsistentOrders.size()) / writeCount * 100 : 100.0
        );

        log.info("数据一致性报告: 写模型 {} 个, 读模型 {} 个, 不一致 {} 个, 一致率 {}%",
                writeCount, readCount, inconsistentOrders.size(), report.get("consistencyRate"));

        return report;
    }
}
