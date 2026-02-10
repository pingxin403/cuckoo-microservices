package com.pingxin403.cuckoo.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.pingxin403.cuckoo.notification",
        "com.pingxin403.cuckoo.common"
})
@EnableDiscoveryClient
@EnableJpaRepositories
public class CuckooNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooNotificationApplication.class, args);
    }

}
