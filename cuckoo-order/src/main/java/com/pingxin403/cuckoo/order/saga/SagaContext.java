package com.pingxin403.cuckoo.order.saga;

import java.util.HashMap;
import java.util.Map;

/**
 * Saga 上下文
 * 用于在 Saga 步骤之间传递数据
 */
public class SagaContext {
    
    private final Map<String, Object> data = new HashMap<>();
    
    /**
     * 设置上下文数据
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    /**
     * 获取上下文数据
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }
    
    /**
     * 获取上下文数据，如果不存在返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }
    
    /**
     * 检查是否包含指定的 key
     */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
    
    /**
     * 获取所有数据
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
    
    /**
     * 从 Map 初始化上下文
     */
    public void putAll(Map<String, Object> data) {
        this.data.putAll(data);
    }
}
