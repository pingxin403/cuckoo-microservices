package com.pingxin403.cuckoo.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡数据源
 * 使用轮询算法在多个从库之间分配连接
 * 支持从库故障自动切换
 * 
 * @author pingxin403
 */
public class LoadBalancedDataSource extends AbstractDataSource {
    
    private static final Logger log = LoggerFactory.getLogger(LoadBalancedDataSource.class);
    
    private final List<DataSourceWrapper> dataSources = new ArrayList<>();
    private final AtomicInteger counter = new AtomicInteger(0);
    private DataSource fallbackDataSource; // 回退数据源（主库）
    
    /**
     * 数据源包装器，包含数据源和健康状态
     */
    private static class DataSourceWrapper {
        private final DataSource dataSource;
        private final String name;
        private volatile boolean healthy = true;
        private volatile int consecutiveFailures = 0;
        private static final int MAX_FAILURES = 3;
        
        public DataSourceWrapper(DataSource dataSource, String name) {
            this.dataSource = dataSource;
            this.name = name;
        }
        
        public DataSource getDataSource() {
            return dataSource;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public void markSuccess() {
            consecutiveFailures = 0;
            if (!healthy) {
                healthy = true;
                log.info("从库 {} 恢复健康", name);
            }
        }
        
        public void markFailure() {
            consecutiveFailures++;
            if (consecutiveFailures >= MAX_FAILURES && healthy) {
                healthy = false;
                log.error("从库 {} 标记为不可用，连续失败次数: {}", name, consecutiveFailures);
            }
        }
    }
    
    /**
     * 添加数据源
     */
    public void addDataSource(DataSource dataSource) {
        String name = "slave-" + (dataSources.size() + 1);
        dataSources.add(new DataSourceWrapper(dataSource, name));
        log.info("添加从库数据源: {}，当前从库数量: {}", name, dataSources.size());
    }
    
    /**
     * 设置回退数据源（主库）
     */
    public void setFallbackDataSource(DataSource fallbackDataSource) {
        this.fallbackDataSource = fallbackDataSource;
        log.info("设置回退数据源（主库）");
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (dataSources.isEmpty()) {
            if (fallbackDataSource != null) {
                log.warn("没有可用的从库数据源，使用主库");
                return fallbackDataSource.getConnection();
            }
            throw new SQLException("没有可用的从库数据源");
        }
        
        // 尝试从健康的从库获取连接
        Connection connection = tryGetConnectionFromSlaves();
        if (connection != null) {
            return connection;
        }
        
        // 所有从库都失败，使用主库作为回退
        if (fallbackDataSource != null) {
            log.warn("所有从库不可用，回退到主库");
            return fallbackDataSource.getConnection();
        }
        
        throw new SQLException("无法获取数据库连接：所有从库不可用且没有配置回退数据源");
    }
    
    /**
     * 尝试从从库获取连接
     */
    private Connection tryGetConnectionFromSlaves() {
        int attempts = dataSources.size() * 2; // 尝试两轮
        
        for (int i = 0; i < attempts; i++) {
            int index = getNextIndex();
            DataSourceWrapper wrapper = dataSources.get(index);
            
            // 跳过不健康的数据源（但每10次尝试一次，用于健康检查）
            if (!wrapper.isHealthy() && i % 10 != 0) {
                continue;
            }
            
            try {
                Connection connection = wrapper.getDataSource().getConnection();
                if (connection != null && !connection.isClosed()) {
                    wrapper.markSuccess();
                    log.debug("成功获取从库连接: {}", wrapper.getName());
                    return connection;
                }
            } catch (SQLException e) {
                wrapper.markFailure();
                log.warn("从库 {} 连接失败: {}", wrapper.getName(), e.getMessage());
            }
        }
        
        return null;
    }
    
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        // 不支持动态用户名密码
        return getConnection();
    }
    
    /**
     * 获取下一个数据源索引（轮询）
     */
    private int getNextIndex() {
        int current = counter.getAndIncrement();
        return Math.abs(current % dataSources.size());
    }
    
    /**
     * 获取数据源数量
     */
    public int getDataSourceCount() {
        return dataSources.size();
    }
    
    /**
     * 获取健康的数据源数量
     */
    public int getHealthyDataSourceCount() {
        return (int) dataSources.stream().filter(DataSourceWrapper::isHealthy).count();
    }
}
