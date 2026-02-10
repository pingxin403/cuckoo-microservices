package com.pingxin403.cuckoo.order.controller;

import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("收到创建订单请求: userId={}, skuId={}, quantity={}",
                request.getUserId(), request.getSkuId(), request.getQuantity());
        OrderDTO order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    /**
     * 查询订单
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        log.info("收到查询订单请求: orderId={}", id);
        OrderDTO order = orderService.getOrder(id);
        return ResponseEntity.ok(order);
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getUserOrders(@PathVariable Long userId) {
        log.info("收到查询用户订单列表请求: userId={}", userId);
        List<OrderDTO> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        log.info("收到取消订单请求: orderId={}", id);
        OrderDTO order = orderService.cancelOrder(id);
        return ResponseEntity.ok(order);
    }
}
