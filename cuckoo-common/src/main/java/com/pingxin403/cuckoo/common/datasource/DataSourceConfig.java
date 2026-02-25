package com.pingxin403.cuckoo.common.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置类
 * 配置主从数据源和动态路由
 * 
 * @author pingxin403
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnProperty(prefix = "spring.datasource", name = "master.jdbc-url")
public class DataSourceConfig {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
    
    private final DataSourceProperties dataSourceProperties;
    
    public DataSourceConfig(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }
    
    /**
     * 创建主库数据源
     */
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        log.info("初始化主库数据源");
        return createDataSource(dataSourceProperties.getMaster(), "master");
    }
    
    /**
     * 创建从库数据源（支持多个从库）
     */
    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        if (dataSourceProperties.getSlaves().isEmpty()) {
            log.warn("未配置从库，读操作将路由到主库");
            return masterDataSource();
        }
        
        log.info("初始化从库数据源，数量: {}", dataSourceProperties.getSlaves().size());
        
        // 如果只有一个从库，直接返回
        if (dataSourceProperties.getSlaves().size() == 1) {
            return createDataSource(dataSourceProperties.getSlaves().get(0), "slave");
        }
        
        // 多个从库时，使用负载均衡数据源
        return createLoadBalancedSlaveDataSource();
    }
    
    /**
     * 创建负载均衡的从库数据源
     */
    private DataSource createLoadBalancedSlaveDataSource() {
        LoadBalancedDataSource loadBalancedDataSource = new LoadBalancedDataSource();
        
        for (int i = 0; i < dataSourceProperties.getSlaves().size(); i++) {
            DataSourceProperties.DataSourceConfig slaveConfig = dataSourceProperties.getSlaves().get(i);
            DataSource slaveDs = createDataSource(slaveConfig, "slave" + (i + 1));
            loadBalancedDataSource.addDataSource(slaveDs);
        }
        
        // 设置主库作为回退数据源
        loadBalancedDataSource.setFallbackDataSource(masterDataSource());
        
        return loadBalancedDataSource;
    }
    
    /**
     * 创建动态数据源
     */
    @Bean(name = "dynamicDataSource")
    @Primary
    public DataSource dynamicDataSource() {
        log.info("初始化动态数据源");
        
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        
        // 设置所有数据源
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER, masterDataSource());
        targetDataSources.put(DataSourceType.SLAVE, slaveDataSource());
        
        dynamicDataSource.setTargetDataSources(targetDataSources);
        
        // 设置默认数据源为主库
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource());
        
        return dynamicDataSource;
    }
    
    /**
     * 配置事务管理器
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dynamicDataSource());
    }
    
    /**
     * 创建 HikariCP 数据源
     */
    private DataSource createDataSource(DataSourceProperties.DataSourceConfig config, String poolName) {
        HikariDataSource dataSource = new HikariDataSource();
        
        dataSource.setJdbcUrl(config.getJdbcUrl());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setDriverClassName(config.getDriverClassName());
        
        // 连接池配置
        dataSource.setPoolName(poolName);
        dataSource.setMaximumPoolSize(config.getMaximumPoolSize());
        dataSource.setMinimumIdle(config.getMinimumIdle());
        dataSource.setConnectionTimeout(config.getConnectionTimeout());
        dataSource.setIdleTimeout(config.getIdleTimeout());
        dataSource.setMaxLifetime(config.getMaxLifetime());
        
        // 连接测试
        dataSource.setConnectionTestQuery("SELECT 1");
        
        log.info("创建数据源: {}, URL: {}", poolName, config.getJdbcUrl());
        
        return dataSource;
    }
}
