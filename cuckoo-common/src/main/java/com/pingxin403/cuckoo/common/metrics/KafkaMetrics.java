package com.pingxin403.cuckoo.common.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka 指标收集器
 * 收集 Kafka 消费者延迟、消息积压等指标
 */
@Slf4j
@Component
@ConditionalOnClass(KafkaTemplate.class)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "spring.kafka.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class KafkaMetrics {

    private final MeterRegistry meterRegistry;
    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
    
    // 存储每个消费者组的延迟时间（秒）
    private final Map<String, AtomicLong> consumerLagSeconds = new ConcurrentHashMap<>();
    
    // 存储每个消费者组的消息积压数量
    private final Map<String, AtomicLong> consumerRecordsLag = new ConcurrentHashMap<>();

    public KafkaMetrics(MeterRegistry meterRegistry, 
                       KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
        this.meterRegistry = meterRegistry;
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
        
        // 注册 Kafka 监听器容器指标
        registerKafkaListenerMetrics();
    }

    /**
     * 注册 Kafka 监听器容器指标
     */
    private void registerKafkaListenerMetrics() {
        // 注册监听器容器数量
        Gauge.builder("kafka_listener_containers_total", 
                kafkaListenerEndpointRegistry, 
                registry -> registry.getListenerContainers().size())
                .description("Kafka 监听器容器总数")
                .register(meterRegistry);
        
        // 注册运行中的监听器容器数量
        Gauge.builder("kafka_listener_containers_running", 
                kafkaListenerEndpointRegistry, 
                this::countRunningContainers)
                .description("运行中的 Kafka 监听器容器数量")
                .register(meterRegistry);
    }

    /**
     * 统计运行中的监听器容器数量
     */
    private long countRunningContainers(KafkaListenerEndpointRegistry registry) {
        return registry.getListenerContainers().stream()
                .filter(MessageListenerContainer::isRunning)
                .count();
    }

    /**
     * 记录 Kafka 消费延迟
     * @param consumerGroup 消费者组
     * @param topic 主题
     * @param lagSeconds 延迟秒数
     */
    public void recordConsumerLag(String consumerGroup, String topic, long lagSeconds) {
        String key = consumerGroup + ":" + topic;
        
        // 如果是第一次记录，注册 Gauge
        consumerLagSeconds.computeIfAbsent(key, k -> {
            AtomicLong lag = new AtomicLong(lagSeconds);
            
            Gauge.builder("kafka_consumer_lag_seconds", lag, AtomicLong::get)
                    .description("Kafka 消费者延迟时间（秒）")
                    .tag("consumer_group", consumerGroup)
                    .tag("topic", topic)
                    .register(meterRegistry);
            
            return lag;
        }).set(lagSeconds);
        
        log.debug("记录 Kafka 消费延迟: consumerGroup={}, topic={}, lagSeconds={}", 
                consumerGroup, topic, lagSeconds);
    }

    /**
     * 记录 Kafka 消息积压数量
     * @param consumerGroup 消费者组
     * @param topic 主题
     * @param partition 分区
     * @param lag 积压消息数
     */
    public void recordConsumerRecordsLag(String consumerGroup, String topic, int partition, long lag) {
        String key = consumerGroup + ":" + topic + ":" + partition;
        
        // 如果是第一次记录，注册 Gauge
        consumerRecordsLag.computeIfAbsent(key, k -> {
            AtomicLong recordsLag = new AtomicLong(lag);
            
            Gauge.builder("kafka_consumer_records_lag", recordsLag, AtomicLong::get)
                    .description("Kafka 消费者消息积压数量")
                    .tag("consumer_group", consumerGroup)
                    .tag("topic", topic)
                    .tag("partition", String.valueOf(partition))
                    .register(meterRegistry);
            
            return recordsLag;
        }).set(lag);
        
        log.debug("记录 Kafka 消息积压: consumerGroup={}, topic={}, partition={}, lag={}", 
                consumerGroup, topic, partition, lag);
    }

    /**
     * 获取消费者延迟时间
     * @param consumerGroup 消费者组
     * @param topic 主题
     * @return 延迟秒数
     */
    public long getConsumerLag(String consumerGroup, String topic) {
        String key = consumerGroup + ":" + topic;
        AtomicLong lag = consumerLagSeconds.get(key);
        return lag != null ? lag.get() : 0;
    }

    /**
     * 获取消息积压数量
     * @param consumerGroup 消费者组
     * @param topic 主题
     * @param partition 分区
     * @return 积压消息数
     */
    public long getConsumerRecordsLag(String consumerGroup, String topic, int partition) {
        String key = consumerGroup + ":" + topic + ":" + partition;
        AtomicLong lag = consumerRecordsLag.get(key);
        return lag != null ? lag.get() : 0;
    }
}
