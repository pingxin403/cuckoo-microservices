package com.pingxin403.cuckoo.common.message;

/**
 * 本地消息状态枚举
 */
public enum MessageStatus {
    /**
     * 待发送
     */
    PENDING,
    
    /**
     * 已发送
     */
    SENT,
    
    /**
     * 发送失败
     */
    FAILED
}
