package com.pingxin403.cuckoo.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.pingxin403.cuckoo.payment",
        "com.pingxin403.cuckoo.common"
})
@EnableJpaRepositories(basePackages = {
        "com.pingxin403.cuckoo.payment.repository",
        "com.pingxin403.cuckoo.common.idempotency"
})
@EntityScan(basePackages = {
        "com.pingxin403.cuckoo.payment.entity",
        "com.pingxin403.cuckoo.common.idempotency"
})
public class CuckooPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooPaymentApplication.class, args);
    }

}
