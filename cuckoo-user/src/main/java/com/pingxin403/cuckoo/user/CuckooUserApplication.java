package com.pingxin403.cuckoo.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.pingxin403.cuckoo.user", "com.pingxin403.cuckoo.common"})
public class CuckooUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooUserApplication.class, args);
    }

}
