package com.pingxin403.cuckoo.product.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流配置
 * 为关键接口配置限流规则
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 商品查询接口限流规则：QPS 50
        FlowRule getProductRule = new FlowRule();
        getProductRule.setResource("GET:/api/products/{id}");
        getProductRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        getProductRule.setCount(50);
        rules.add(getProductRule);

        // 加载限流规则
        FlowRuleManager.loadRules(rules);
        log.info("Sentinel flow rules initialized for Product Service: {} rules loaded", rules.size());
    }
}
