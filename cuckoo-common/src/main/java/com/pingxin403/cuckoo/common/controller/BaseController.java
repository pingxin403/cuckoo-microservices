package com.pingxin403.cuckoo.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 基础控制器抽象类
 * 提供统一的响应包装和日志记录方法，减少 Controller 层的重复代码
 * 
 * @author pingxin403
 */
@Slf4j
public abstract class BaseController {

    /**
     * 返回 201 Created 响应
     * 
     * @param body 响应体
     * @param <T> 响应体类型
     * @return ResponseEntity with 201 status
     */
    protected <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * 返回 200 OK 响应
     * 
     * @param body 响应体
     * @param <T> 响应体类型
     * @return ResponseEntity with 200 status
     */
    protected <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    /**
     * 返回 204 No Content 响应
     * 
     * @return ResponseEntity with 204 status
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * 记录请求日志
     * 
     * @param operation 操作名称
     * @param params 请求参数
     */
    protected void logRequest(String operation, Object... params) {
        if (params.length == 0) {
            log.info("请求: {}", operation);
        } else {
            log.info("请求: {}, 参数: {}", operation, params);
        }
    }

    /**
     * 记录响应日志
     * 
     * @param operation 操作名称
     * @param result 响应结果
     */
    protected void logResponse(String operation, Object result) {
        log.info("响应: {}, 结果: {}", operation, result);
    }
}
