package com.pingxin403.cuckoo.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.pingxin403.cuckoo.payment",
        "com.pingxin403.cuckoo.common"
})
@EnableJpaRepositories
public class CuckooPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooPaymentApplication.class, args);
    }

}
