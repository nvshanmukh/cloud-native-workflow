package com.example.workflow.kafka.consumer;

import com.example.workflow.entity.Task;
import com.example.workflow.entity.TaskStatus;
import com.example.workflow.kafka.config.KafkaTopicConfig;
import com.example.workflow.orchestration.OrchestrationService;
import com.example.workflow.repository.TaskRepository;
import com.example.workflow.worker.TaskExecutor;
import com.example.workflow.worker.WorkerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final OrchestrationService orchestrationService;
    private final WorkerRegistry workerRegistry;
    private final TaskRepository taskRepository;

    @KafkaListener(topics = KafkaTopicConfig.TASK_READY_TOPIC, groupId = "workflow-worker-group")
    public void consumeTaskReady(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Worker received task from Kafka: {}", event);
        
        UUID taskId = null;
        try {
            taskId = UUID.fromString((String) event.get("taskId"));
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));

            if (task.getStatus() != TaskStatus.READY && task.getStatus() != TaskStatus.RETRYING) {
                log.warn("Task {} is in state {}, discarding message", taskId, task.getStatus());
                acknowledgment.acknowledge();
                return;
            }

            // Mark as RUNNING
            task.setStatus(TaskStatus.RUNNING);
            taskRepository.save(task);

            // Execute
            TaskExecutor executor = workerRegistry.getExecutor(task.getTaskType());
            String result = executor.execute(task);

            // Complete in Orchestrator
            orchestrationService.handleTaskCompletion(taskId, result);

            // ONLY acknowledge if everything succeeds
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process task event", e);
            
            // TODO: Milestone 7 - Reliability (Retries, Dead Letter Queue)
            // For now, fail it permanently
            if (taskId != null) {
                orchestrationService.handleTaskFailure(taskId, e.getMessage());
                acknowledgment.acknowledge(); // Acknowledge so we don't infinitely loop on error yet
            }
        }
    }
}
