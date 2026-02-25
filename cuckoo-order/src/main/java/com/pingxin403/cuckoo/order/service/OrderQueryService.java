package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.entity.OrderRead;
import com.pingxin403.cuckoo.order.repository.OrderReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单查询服务（CQRS 读模型）
 * 使用 order_read 表进行查询，优化查询性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderReadRepository orderReadRepository;

    /**
     * 根据订单 ID 查询订单详情（从读模型）
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(String orderId) {
        log.info("查询订单详情（读模型）: orderId={}", orderId);

        OrderRead orderRead = orderReadRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

        return convertToDTO(orderRead);
    }

    /**
     * 查询用户订单列表（从读模型）
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders(Long userId) {
        log.info("查询用户订单列表（读模型）: userId={}", userId);

        List<OrderRead> orders = orderReadRepository.findByUserId(userId);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户订单列表（分页，从读模型）
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getUserOrdersPage(Long userId, int page, int size) {
        log.info("查询用户订单列表（分页，读模型）: userId={}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderRead> ordersPage = orderReadRepository.findByUserId(userId, pageable);

        return ordersPage.map(this::convertToDTO);
    }

    /**
     * 根据状态查询订单列表（从读模型）
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(String status) {
        log.info("根据状态查询订单列表（读模型）: status={}", status);

        List<OrderRead> orders = orderReadRepository.findByStatus(status);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户指定状态的订单列表（从读模型）
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrdersByStatus(Long userId, String status) {
        log.info("查询用户指定状态的订单列表（读模型）: userId={}, status={}", userId, status);

        List<OrderRead> orders = orderReadRepository.findByUserIdAndStatus(userId, status);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为 DTO
     */
    private OrderDTO convertToDTO(OrderRead orderRead) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(orderRead.getOrderId());
        dto.setUserId(orderRead.getUserId());
        dto.setUserName(orderRead.getUserName());
        dto.setTotalAmount(orderRead.getTotalAmount());
        dto.setStatus(orderRead.getStatus());
        dto.setStatusDisplay(orderRead.getStatusDisplay());
        dto.setItemCount(orderRead.getItemCount());
        dto.setProductNames(orderRead.getProductNames());
        dto.setSkuIds(orderRead.getSkuIds());
        dto.setPaymentId(orderRead.getPaymentId());
        dto.setCancelReason(orderRead.getCancelReason());
        dto.setCreatedAt(orderRead.getCreatedAt());
        dto.setUpdatedAt(orderRead.getUpdatedAt());
        return dto;
    }
}
