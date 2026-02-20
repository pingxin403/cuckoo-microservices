package com.pingxin403.cuckoo.common.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.jqwik.api.*;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseController 日志属性测试
 * 
 * 验证属性 2：BaseController 日志方法记录正确的信息
 * - logRequest() 记录包含操作名称和参数的日志
 * - logResponse() 记录包含操作名称和结果的日志
 */
class BaseControllerLoggingPropertyTest {

    /**
     * 测试控制器，用于测试 BaseController 的日志方法
     */
    private static class TestController extends BaseController {
        public void testLogRequest(String operation, Object... params) {
            logRequest(operation, params);
        }
        
        public void testLogResponse(String operation, Object result) {
            logResponse(operation, result);
        }
    }

    @Property(tries = 100)
    @Label("Feature: microservice-optimization, Property 2: BaseController 日志方法记录正确的信息")
    void baseController_loggingMethods_logCorrectInformation(
            @ForAll String operation,
            @ForAll String param1,
            @ForAll String param2,
            @ForAll String result) {
        
        TestController controller = new TestController();
        
        // 设置日志捕获器
        Logger logger = (Logger) LoggerFactory.getLogger(BaseController.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        try {
            // 测试 logRequest() 方法
            controller.testLogRequest(operation, param1, param2);
            
            // 验证日志包含操作名称和参数
            assertThat(listAppender.list).isNotEmpty();
            ILoggingEvent requestLog = listAppender.list.get(listAppender.list.size() - 1);
            assertThat(requestLog.getFormattedMessage()).contains(operation);
            assertThat(requestLog.getFormattedMessage()).contains(param1);
            assertThat(requestLog.getFormattedMessage()).contains(param2);
            
            // 清空日志
            listAppender.list.clear();
            
            // 测试 logResponse() 方法
            controller.testLogResponse(operation, result);
            
            // 验证日志包含操作名称和结果
            assertThat(listAppender.list).isNotEmpty();
            ILoggingEvent responseLog = listAppender.list.get(listAppender.list.size() - 1);
            assertThat(responseLog.getFormattedMessage()).contains(operation);
            assertThat(responseLog.getFormattedMessage()).contains(result);
            
        } finally {
            // 清理
            logger.detachAppender(listAppender);
        }
    }
    
    @Property(tries = 100)
    @Label("Feature: microservice-optimization, Property 2: BaseController logRequest 无参数时记录正确信息")
    void baseController_logRequest_withoutParams_logsCorrectly(
            @ForAll String operation) {
        
        TestController controller = new TestController();
        
        // 设置日志捕获器
        Logger logger = (Logger) LoggerFactory.getLogger(BaseController.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        try {
            // 测试 logRequest() 方法（无参数）
            controller.testLogRequest(operation);
            
            // 验证日志包含操作名称
            assertThat(listAppender.list).isNotEmpty();
            ILoggingEvent requestLog = listAppender.list.get(listAppender.list.size() - 1);
            assertThat(requestLog.getFormattedMessage()).contains(operation);
            
        } finally {
            // 清理
            logger.detachAppender(listAppender);
        }
    }
}
