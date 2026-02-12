package com.pingxin403.cuckoo.payment.config;

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

        // 支付接口限流规则：QPS 10
        FlowRule processPaymentRule = new FlowRule();
        processPaymentRule.setResource("POST:/api/payments");
        processPaymentRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        processPaymentRule.setCount(10);
        rules.add(processPaymentRule);

        // 加载限流规则
        FlowRuleManager.loadRules(rules);
        log.info("Sentinel flow rules initialized for Payment Service: {} rules loaded", rules.size());
    }
}
