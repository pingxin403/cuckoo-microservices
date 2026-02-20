package com.pingxin403.cuckoo.common.controller;

import net.jqwik.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseController 属性测试
 * 
 * 验证属性 1：BaseController 响应方法返回正确的 HTTP 状态码
 * - created() 方法返回 201 Created
 * - ok() 方法返回 200 OK
 * - noContent() 方法返回 204 No Content
 */
class BaseControllerPropertyTest {

    /**
     * 测试控制器，用于测试 BaseController 的方法
     */
    private static class TestController extends BaseController {
        public <T> ResponseEntity<T> testCreated(T body) {
            return created(body);
        }
        
        public <T> ResponseEntity<T> testOk(T body) {
            return ok(body);
        }
        
        public ResponseEntity<Void> testNoContent() {
            return noContent();
        }
    }

    @Property(tries = 100)
    @Label("Feature: microservice-optimization, Property 1: BaseController 响应方法返回正确的 HTTP 状态码")
    void baseController_responseMethods_returnCorrectStatusCodes(@ForAll String body) {
        TestController controller = new TestController();
        
        // 测试 created() 方法返回 201 Created
        ResponseEntity<String> createdResponse = controller.testCreated(body);
        assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createdResponse.getBody()).isEqualTo(body);
        
        // 测试 ok() 方法返回 200 OK
        ResponseEntity<String> okResponse = controller.testOk(body);
        assertThat(okResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(okResponse.getBody()).isEqualTo(body);
        
        // 测试 noContent() 方法返回 204 No Content
        ResponseEntity<Void> noContentResponse = controller.testNoContent();
        assertThat(noContentResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(noContentResponse.getBody()).isNull();
    }
}
