package com.example.workflow.kafka.producer;

import com.example.workflow.entity.Task;
import com.example.workflow.kafka.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTaskReady(Task task) {
        Map<String, Object> event = new HashMap<>();
        event.put("taskId", task.getId());
        event.put("workflowId", task.getWorkflow().getId());
        event.put("taskType", task.getTaskType());
        event.put("payload", task.getPayload());
        
        log.info("Publishing task {} to READY topic", task.getId());
        kafkaTemplate.send(KafkaTopicConfig.TASK_READY_TOPIC, task.getId().toString(), event);
    }

    public void publishTaskRetry(Task task) {
        Map<String, Object> event = new HashMap<>();
        event.put("taskId", task.getId());
        event.put("retryCount", task.getRetryCount());
        
        log.info("Publishing task {} to RETRY topic", task.getId());
        kafkaTemplate.send(KafkaTopicConfig.TASK_RETRY_TOPIC, task.getId().toString(), event);
    }
}
