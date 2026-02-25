package com.pingxin403.cuckoo.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据源上下文持有者
 * 使用 ThreadLocal 存储当前线程的数据源类型
 * 
 * @author pingxin403
 */
public class DataSourceContextHolder {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceContextHolder.class);
    
    private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();
    
    /**
     * 设置数据源类型
     * 
     * @param dataSourceType 数据源类型
     */
    public static void setDataSourceType(DataSourceType dataSourceType) {
        if (dataSourceType == null) {
            log.warn("尝试设置 null 数据源类型，将使用默认主库");
            dataSourceType = DataSourceType.MASTER;
        }
        log.debug("切换数据源到: {}", dataSourceType);
        contextHolder.set(dataSourceType);
    }
    
    /**
     * 获取数据源类型
     * 
     * @return 数据源类型，如果未设置则返回 MASTER
     */
    public static DataSourceType getDataSourceType() {
        DataSourceType type = contextHolder.get();
        if (type == null) {
            log.debug("未设置数据源类型，使用默认主库");
            return DataSourceType.MASTER;
        }
        return type;
    }
    
    /**
     * 清除数据源类型
     * 必须在请求结束时调用，避免线程池复用导致的数据源污染
     */
    public static void clearDataSourceType() {
        log.debug("清除数据源上下文");
        contextHolder.remove();
    }
    
    /**
     * 强制使用主库
     * 用于写后读场景，确保读取到最新数据
     */
    public static void forceMaster() {
        log.debug("强制使用主库");
        contextHolder.set(DataSourceType.MASTER);
    }
    
    /**
     * 使用从库（如果可用）
     * 用于只读查询场景
     */
    public static void useSlave() {
        log.debug("使用从库");
        contextHolder.set(DataSourceType.SLAVE);
    }
}
