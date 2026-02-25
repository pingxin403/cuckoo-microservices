package com.pingxin403.cuckoo.common.datasource;

/**
 * 数据源类型枚举
 * 
 * @author pingxin403
 */
public enum DataSourceType {
    /**
     * 主库 - 用于写操作和事务
     */
    MASTER,
    
    /**
     * 从库 - 用于读操作
     */
    SLAVE
}
