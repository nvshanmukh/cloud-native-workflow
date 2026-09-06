package com.example.workflow.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TASK_READY_TOPIC = "workflow.task.ready";
    public static final String TASK_COMPLETED_TOPIC = "workflow.task.completed";
    public static final String TASK_FAILED_TOPIC = "workflow.task.failed";
    public static final String TASK_RETRY_TOPIC = "workflow.task.retry";

    @Bean
    public NewTopic taskReadyTopic() {
        return TopicBuilder.name(TASK_READY_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskCompletedTopic() {
        return TopicBuilder.name(TASK_COMPLETED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic taskFailedTopic() {
        return TopicBuilder.name(TASK_FAILED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskRetryTopic() {
        return TopicBuilder.name(TASK_RETRY_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
