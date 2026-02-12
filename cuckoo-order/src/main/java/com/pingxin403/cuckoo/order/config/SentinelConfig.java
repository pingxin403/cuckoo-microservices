package com.pingxin403.cuckoo.order.config;

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

        // 订单创建接口限流规则：QPS 20，线程数 5
        FlowRule createOrderQpsRule = new FlowRule();
        createOrderQpsRule.setResource("POST:/api/orders");
        createOrderQpsRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createOrderQpsRule.setCount(20);
        rules.add(createOrderQpsRule);

        FlowRule createOrderThreadRule = new FlowRule();
        createOrderThreadRule.setResource("POST:/api/orders");
        createOrderThreadRule.setGrade(RuleConstant.FLOW_GRADE_THREAD);
        createOrderThreadRule.setCount(5);
        rules.add(createOrderThreadRule);

        // 加载限流规则
        FlowRuleManager.loadRules(rules);
        log.info("Sentinel flow rules initialized for Order Service: {} rules loaded", rules.size());
    }
}
