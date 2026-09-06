package com.example.workflow.worker;

import com.example.workflow.entity.Task;
import com.example.workflow.entity.TaskStatus;
import com.example.workflow.kafka.producer.KafkaProducerService;
import com.example.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RetrySweeper {

    private final TaskRepository taskRepository;
    private final KafkaProducerService kafkaProducerService;

    // Runs every 10 seconds
    @Scheduled(fixedDelay = 10000)
    public void sweepRetryingTasks() {
        List<Task> retryingTasks = taskRepository.findByStatus(TaskStatus.RETRYING);
        
        for (Task task : retryingTasks) {
            // Exponential backoff: base_delay * 2^(retry_count - 1)
            // base_delay = 10 seconds
            long backoffSeconds = 10L * (long) Math.pow(2, task.getRetryCount() - 1);
            Instant readyTime = task.getUpdatedAt().plusSeconds(backoffSeconds);
            
            if (Instant.now().isAfter(readyTime)) {
                log.info("Backoff complete for task {}. Re-queueing to READY.", task.getId());
                task.setStatus(TaskStatus.READY);
                taskRepository.save(task);
                
                kafkaProducerService.publishTaskReady(task);
            }
        }
    }
}
