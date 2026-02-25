package com.pingxin403.cuckoo.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由
 * 根据 DataSourceContextHolder 中的数据源类型动态选择数据源
 * 
 * @author pingxin403
 */
public class DynamicDataSource extends AbstractRoutingDataSource {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSource.class);
    
    /**
     * 确定当前数据源的查找键
     * 
     * @return 数据源类型
     */
    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType dataSourceType = DataSourceContextHolder.getDataSourceType();
        log.debug("当前数据源: {}", dataSourceType);
        return dataSourceType;
    }
    
    /**
     * 在数据源查找失败时的回退逻辑
     * 如果从库不可用，回退到主库
     */
    @Override
    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        if (lookupKey instanceof DataSourceType) {
            return lookupKey;
        }
        log.warn("无效的数据源类型: {}, 使用主库", lookupKey);
        return DataSourceType.MASTER;
    }
}
