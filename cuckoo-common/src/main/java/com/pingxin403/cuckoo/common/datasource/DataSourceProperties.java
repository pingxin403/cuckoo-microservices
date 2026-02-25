package com.pingxin403.cuckoo.common.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据源配置属性
 * 
 * @author pingxin403
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {
    
    private DataSourceConfig master;
    private List<DataSourceConfig> slaves = new ArrayList<>();
    
    public DataSourceConfig getMaster() {
        return master;
    }
    
    public void setMaster(DataSourceConfig master) {
        this.master = master;
    }
    
    public List<DataSourceConfig> getSlaves() {
        return slaves;
    }
    
    public void setSlaves(List<DataSourceConfig> slaves) {
        this.slaves = slaves;
    }
    
    /**
     * 单个数据源配置
     */
    public static class DataSourceConfig {
        private String jdbcUrl;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        
        // HikariCP 连接池配置
        private int maximumPoolSize = 10;
        private int minimumIdle = 5;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        
        public String getJdbcUrl() {
            return jdbcUrl;
        }
        
        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getDriverClassName() {
            return driverClassName;
        }
        
        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
        
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }
        
        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }
        
        public int getMinimumIdle() {
            return minimumIdle;
        }
        
        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
        
        public long getConnectionTimeout() {
            return connectionTimeout;
        }
        
        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        public long getIdleTimeout() {
            return idleTimeout;
        }
        
        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }
        
        public long getMaxLifetime() {
            return maxLifetime;
        }
        
        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }
    }
}
