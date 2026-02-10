package com.pingxin403.cuckoo.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.pingxin403.cuckoo.inventory",
        "com.pingxin403.cuckoo.common"
})
@EntityScan(basePackages = {
        "com.pingxin403.cuckoo.inventory.entity",
        "com.pingxin403.cuckoo.common.idempotency"
})
@EnableJpaRepositories(basePackages = {
        "com.pingxin403.cuckoo.inventory.repository",
        "com.pingxin403.cuckoo.common.idempotency"
})
public class CuckooInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooInventoryApplication.class, args);
    }

}
