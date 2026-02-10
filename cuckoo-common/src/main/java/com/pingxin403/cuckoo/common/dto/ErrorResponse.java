package com.pingxin403.cuckoo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 统一错误响应 DTO
 * 所有异常通过 GlobalExceptionHandler 转换为此格式返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * 错误码（如 NOT_FOUND, CONFLICT, BAD_REQUEST, INTERNAL_ERROR 等）
     */
    private String error;

    /**
     * 错误描述信息
     */
    private String message;

    /**
     * 错误发生时间
     */
    private Instant timestamp;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
