package com.pingxin403.cuckoo.common.audit;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * 标记需要记录审计日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * 操作类型
     */
    AuditLog.OperationType value();

    /**
     * 资源类型
     */
    String resourceType() default "";

    /**
     * 操作描述
     */
    String description() default "";
}
