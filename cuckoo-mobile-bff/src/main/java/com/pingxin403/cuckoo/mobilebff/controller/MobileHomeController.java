package com.pingxin403.cuckoo.mobilebff.controller;

import com.pingxin403.cuckoo.mobilebff.dto.HomePageResponse;
import com.pingxin403.cuckoo.mobilebff.service.MobileHomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 移动端主页控制器
 */
@Slf4j
@RestController
@RequestMapping("/mobile/api")
public class MobileHomeController {

    @Autowired
    private MobileHomeService homeService;

    /**
     * 获取移动端主页数据
     * 聚合用户信息、最近订单和未读通知
     */
    @GetMapping("/home")
    public CompletableFuture<HomePageResponse> getHomePage(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching home page data for user: {}", userId);
        
        return homeService.aggregateHomePage(userId)
            .orTimeout(3, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                log.error("Failed to fetch home page data for user: {}", userId, ex);
                return homeService.buildDegradedResponse(userId);
            });
    }

}
