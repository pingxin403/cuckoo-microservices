package com.pingxin403.cuckoo.common.datasource;

import java.lang.annotation.*;

/**
 * 只读操作注解
 * 标记在方法上，表示该方法只进行读操作，可以路由到从库
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @ReadOnly
 * public User getUserById(Long id) {
 *     return userRepository.findById(id).orElse(null);
 * }
 * }
 * </pre>
 * 
 * @author pingxin403
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
    
    /**
     * 是否强制使用主库
     * 某些场景下即使是读操作也需要从主库读取，例如：
     * - 写后立即读
     * - 对数据一致性要求极高的场景
     * 
     * @return true 强制使用主库，false 使用从库
     */
    boolean forceMaster() default false;
}
