package com.example.workflow.orchestration;

import com.example.workflow.entity.Task;
import com.example.workflow.entity.TaskAttempt;
import com.example.workflow.entity.TaskStatus;
import com.example.workflow.entity.Workflow;
import com.example.workflow.entity.WorkflowStatus;
import com.example.workflow.kafka.producer.KafkaProducerService;
import com.example.workflow.repository.TaskAttemptRepository;
import com.example.workflow.repository.TaskRepository;
import com.example.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestrationService {

    private final TaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    @CacheEvict(value = {"workflow", "workflows"}, allEntries = true)
    public void handleTaskCompletion(UUID taskId, String result) {
        log.info("Handling completion for task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() == TaskStatus.COMPLETED) {
            log.warn("Task {} is already completed. Idempotent return.", taskId);
            return; // Idempotent
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setResult(result);
        task.setCompletedAt(Instant.now());
        taskRepository.save(task);

        evaluateDependentTasks(task);
        evaluateWorkflowStatus(task.getWorkflow().getId());
    }

    @Transactional
    @CacheEvict(value = {"workflow", "workflows"}, allEntries = true)
    public void handleTaskFailure(UUID taskId, String error) {
        log.error("Handling failure for task: {}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
                
        // Record attempt
        int attempt = task.getRetryCount() + 1;
        TaskAttempt taskAttempt = TaskAttempt.builder()
                .task(task)
                .attemptNumber(attempt)
                .errorMessage(error)
                .completedAt(Instant.now())
                .status(TaskStatus.FAILED)
                .build();
        taskAttemptRepository.save(taskAttempt);

        if (task.getRetryCount() < task.getMaxRetries()) {
            // RETRY
            task.setRetryCount(attempt);
            task.setStatus(TaskStatus.RETRYING);
            taskRepository.save(task);
            
            // Publish to Retry Topic
            kafkaProducerService.publishTaskRetry(task);
            log.info("Task {} scheduled for retry {}", taskId, attempt);
        } else {
            // DEAD LETTER
            log.error("Task {} exhausted retries. Marking DEAD_LETTER.", taskId);
            task.setStatus(TaskStatus.DEAD_LETTER);
            task.setResult(error);
            task.setCompletedAt(Instant.now());
            taskRepository.save(task);

            // Fail the workflow
            Workflow workflow = task.getWorkflow();
            workflow.setStatus(WorkflowStatus.FAILED);
            workflow.setCompletedAt(Instant.now());
            workflowRepository.save(workflow);
            
            cancelRemainingTasks(workflow.getId());
        }
    }

    private void evaluateDependentTasks(Task completedTask) {
        List<Task> dependentTasks = taskRepository.findDependentTasks(completedTask.getId());

        for (Task dependentTask : dependentTasks) {
            if (dependentTask.getStatus() != TaskStatus.PENDING) {
                continue;
            }

            boolean allDependenciesCompleted = true;
            for (Task dep : dependentTask.getDependencies()) {
                if (dep.getStatus() != TaskStatus.COMPLETED) {
                    allDependenciesCompleted = false;
                    break;
                }
            }

            if (allDependenciesCompleted) {
                log.info("All dependencies satisfied for task {}. Marking as READY.", dependentTask.getId());
                dependentTask.setStatus(TaskStatus.READY);
                taskRepository.save(dependentTask);
                
                kafkaProducerService.publishTaskReady(dependentTask);
            }
        }
    }

    private void evaluateWorkflowStatus(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        
        boolean allTasksCompleted = workflow.getTasks().stream()
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (allTasksCompleted && workflow.getStatus() != WorkflowStatus.COMPLETED) {
            log.info("All tasks completed for workflow {}. Marking workflow as COMPLETED.", workflowId);
            workflow.setStatus(WorkflowStatus.COMPLETED);
            workflow.setCompletedAt(Instant.now());
            workflowRepository.save(workflow);
        } else if (workflow.getStatus() == WorkflowStatus.CREATED) {
            workflow.setStatus(WorkflowStatus.RUNNING);
            workflow.setStartedAt(Instant.now());
            workflowRepository.save(workflow);
        }
    }

    private void cancelRemainingTasks(UUID workflowId) {
        List<Task> pendingTasks = taskRepository.findByWorkflowId(workflowId).stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.READY)
                .collect(Collectors.toList());
                
        for (Task task : pendingTasks) {
            task.setStatus(TaskStatus.FAILED);
            task.setResult("Cancelled due to sibling task failure");
            taskRepository.save(task);
        }
    }
}
