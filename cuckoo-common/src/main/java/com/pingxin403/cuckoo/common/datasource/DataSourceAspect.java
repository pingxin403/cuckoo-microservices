package com.pingxin403.cuckoo.common.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * 数据源切换切面
 * 根据方法注解和事务状态自动切换数据源
 * 
 * 路由规则：
 * 1. @Transactional 注解的方法 -> 主库
 * 2. @ReadOnly 注解的方法 -> 从库（除非 forceMaster=true）
 * 3. 写后读场景 -> 主库（确保读取到最新数据）
 * 4. 方法名以 get/find/query/select/count/list 开头 -> 从库
 * 5. 其他方法 -> 主库
 * 
 * @author pingxin403
 */
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class DataSourceAspect {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceAspect.class);
    
    @Autowired(required = false)
    private WriteAfterReadDetector writeAfterReadDetector;
    
    /**
     * 定义切点：所有 Service 层的方法
     */
    @Pointcut("execution(* com.pingxin403.cuckoo..service..*.*(..))")
    public void serviceMethod() {
    }
    
    /**
     * 环绕通知：在方法执行前设置数据源，执行后清除
     */
    @Around("serviceMethod()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        try {
            // 确定数据源类型
            DataSourceType dataSourceType = determineDataSourceType(method, joinPoint.getArgs());
            
            // 设置数据源
            DataSourceContextHolder.setDataSourceType(dataSourceType);
            
            log.debug("方法 {} 使用数据源: {}", method.getName(), dataSourceType);
            
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 如果是写操作，记录写入
            if (dataSourceType == DataSourceType.MASTER && !isReadMethod(method.getName())) {
                recordWriteOperation(method, joinPoint.getArgs());
            }
            
            return result;
            
        } finally {
            // 清除数据源上下文
            DataSourceContextHolder.clearDataSourceType();
        }
    }
    
    /**
     * 确定数据源类型
     * 
     * @param method 目标方法
     * @param args 方法参数
     * @return 数据源类型
     */
    private DataSourceType determineDataSourceType(Method method, Object[] args) {
        // 1. 检查是否有 @Transactional 注解（方法级别或类级别）
        if (method.isAnnotationPresent(Transactional.class) || 
            method.getDeclaringClass().isAnnotationPresent(Transactional.class)) {
            log.debug("检测到 @Transactional 注解，使用主库");
            return DataSourceType.MASTER;
        }
        
        // 2. 检查是否有 @ReadOnly 注解（方法级别）
        if (method.isAnnotationPresent(ReadOnly.class)) {
            ReadOnly readOnly = method.getAnnotation(ReadOnly.class);
            if (readOnly.forceMaster()) {
                log.debug("检测到 @ReadOnly(forceMaster=true) 注解，使用主库");
                return DataSourceType.MASTER;
            }
            
            // 检查写后读场景
            if (writeAfterReadDetector != null && isWriteAfterReadScenario(method, args)) {
                log.debug("检测到写后读场景，强制使用主库");
                return DataSourceType.MASTER;
            }
            
            log.debug("检测到 @ReadOnly 注解，使用从库");
            return DataSourceType.SLAVE;
        }
        
        // 3. 检查类级别的 @ReadOnly 注解
        if (method.getDeclaringClass().isAnnotationPresent(ReadOnly.class)) {
            ReadOnly readOnly = method.getDeclaringClass().getAnnotation(ReadOnly.class);
            if (readOnly.forceMaster()) {
                log.debug("检测到类级别 @ReadOnly(forceMaster=true) 注解，使用主库");
                return DataSourceType.MASTER;
            }
            
            // 检查写后读场景
            if (writeAfterReadDetector != null && isWriteAfterReadScenario(method, args)) {
                log.debug("检测到写后读场景，强制使用主库");
                return DataSourceType.MASTER;
            }
            
            log.debug("检测到类级别 @ReadOnly 注解，使用从库");
            return DataSourceType.SLAVE;
        }
        
        // 4. 根据方法名判断
        String methodName = method.getName();
        if (isReadMethod(methodName)) {
            // 检查写后读场景
            if (writeAfterReadDetector != null && isWriteAfterReadScenario(method, args)) {
                log.debug("检测到写后读场景，强制使用主库");
                return DataSourceType.MASTER;
            }
            
            log.debug("方法名 {} 表示读操作，使用从库", methodName);
            return DataSourceType.SLAVE;
        }
        
        // 5. 默认使用主库
        log.debug("默认使用主库");
        return DataSourceType.MASTER;
    }
    
    /**
     * 判断是否为读方法
     * 
     * @param methodName 方法名
     * @return true 如果是读方法
     */
    private boolean isReadMethod(String methodName) {
        return methodName.startsWith("get") ||
               methodName.startsWith("find") ||
               methodName.startsWith("query") ||
               methodName.startsWith("select") ||
               methodName.startsWith("count") ||
               methodName.startsWith("list") ||
               methodName.startsWith("search") ||
               methodName.startsWith("exists");
    }
    
    /**
     * 检查是否为写后读场景
     * 
     * @param method 方法
     * @param args 参数
     * @return true 如果是写后读场景
     */
    private boolean isWriteAfterReadScenario(Method method, Object[] args) {
        if (writeAfterReadDetector == null) {
            return false;
        }
        
        // 尝试从方法参数中提取资源ID
        String resourceId = extractResourceId(method, args);
        if (resourceId != null) {
            String resourceType = extractResourceType(method);
            return writeAfterReadDetector.isWriteAfterRead(resourceType, resourceId);
        }
        
        return false;
    }
    
    /**
     * 记录写操作
     * 
     * @param method 方法
     * @param args 参数
     */
    private void recordWriteOperation(Method method, Object[] args) {
        if (writeAfterReadDetector == null) {
            return;
        }
        
        String resourceId = extractResourceId(method, args);
        if (resourceId != null) {
            String resourceType = extractResourceType(method);
            writeAfterReadDetector.recordWrite(resourceType, resourceId);
        }
    }
    
    /**
     * 从方法参数中提取资源ID
     * 支持：Long/String 类型的 id 参数，或包含 getId() 方法的对象
     */
    private String extractResourceId(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        
        // 检查第一个参数
        Object firstArg = args[0];
        if (firstArg instanceof Long || firstArg instanceof String) {
            return firstArg.toString();
        }
        
        // 尝试调用 getId() 方法
        try {
            Method getIdMethod = firstArg.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(firstArg);
            if (id != null) {
                return id.toString();
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return null;
    }
    
    /**
     * 从方法所在类提取资源类型
     * 例如：OrderService -> order
     */
    private String extractResourceType(Method method) {
        String className = method.getDeclaringClass().getSimpleName();
        // 移除 "Service" 后缀并转换为小写
        return className.replace("Service", "").toLowerCase();
    }
}

