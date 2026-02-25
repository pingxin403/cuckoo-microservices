package com.pingxin403.cuckoo.common.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表Repository
 */
@Repository
public interface LocalMessageRepository extends JpaRepository<LocalMessage, String> {
    
    /**
     * 查询待发送的消息
     * 
     * @param status 消息状态
     * @param limit 限制数量
     * @return 待发送的消息列表
     */
    @Query("SELECT m FROM LocalMessage m WHERE m.status = :status ORDER BY m.createdAt ASC")
    List<LocalMessage> findPendingMessages(@Param("status") MessageStatus status, @Param("limit") int limit);
    
    /**
     * 查询指定时间之前已发送的消息
     * 
     * @param status 消息状态
     * @param beforeTime 时间阈值
     * @return 旧消息列表
     */
    List<LocalMessage> findByStatusAndSentAtBefore(MessageStatus status, LocalDateTime beforeTime);
    
    /**
     * 统计指定状态的消息数量
     * 
     * @param status 消息状态
     * @return 消息数量
     */
    long countByStatus(MessageStatus status);
}
