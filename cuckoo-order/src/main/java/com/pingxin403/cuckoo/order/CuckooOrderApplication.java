package com.pingxin403.cuckoo.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.pingxin403.cuckoo.order",
        "com.pingxin403.cuckoo.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableKafka
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.pingxin403.cuckoo.order.repository",
        "com.pingxin403.cuckoo.common.idempotency"
})
@EntityScan(basePackages = {
        "com.pingxin403.cuckoo.order.entity",
        "com.pingxin403.cuckoo.common.idempotency"
})
public class CuckooOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooOrderApplication.class, args);
    }

}
