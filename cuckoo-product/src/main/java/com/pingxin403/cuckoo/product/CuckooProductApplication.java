package com.pingxin403.cuckoo.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.pingxin403.cuckoo.product", "com.pingxin403.cuckoo.common"})
public class CuckooProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooProductApplication.class, args);
    }

}
