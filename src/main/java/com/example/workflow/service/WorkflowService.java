package com.example.workflow.service;

import com.example.workflow.dto.TaskCreateRequest;
import com.example.workflow.dto.TaskResponse;
import com.example.workflow.dto.WorkflowCreateRequest;
import com.example.workflow.dto.WorkflowResponse;
import com.example.workflow.entity.Task;
import com.example.workflow.entity.TaskStatus;
import com.example.workflow.entity.User;
import com.example.workflow.entity.Workflow;
import com.example.workflow.kafka.producer.KafkaProducerService;
import com.example.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    @CacheEvict(value = "workflows", key = "#user.id")
    public WorkflowResponse createWorkflow(WorkflowCreateRequest request, User user) {
        Workflow workflow = Workflow.builder()
                .name(request.getName())
                .user(user)
                .build();

        // Pass 1: Create all Task objects mapped by their reference ID
        Map<String, Task> taskMap = new HashMap<>();
        for (TaskCreateRequest taskReq : request.getTasks()) {
            if (taskMap.containsKey(taskReq.getReferenceId())) {
                throw new IllegalArgumentException("Duplicate task reference ID: " + taskReq.getReferenceId());
            }
            Task task = Task.builder()
                    .workflow(workflow)
                    .taskType(taskReq.getTaskType())
                    .payload(taskReq.getPayload())
                    .priority(taskReq.getPriority())
                    .maxRetries(taskReq.getMaxRetries())
                    .timeoutSeconds(taskReq.getTimeoutSeconds())
                    .build();
            taskMap.put(taskReq.getReferenceId(), task);
            workflow.getTasks().add(task);
        }

        // Pass 2: Wire dependencies
        for (TaskCreateRequest taskReq : request.getTasks()) {
            Task task = taskMap.get(taskReq.getReferenceId());
            if (taskReq.getDependsOnReferenceIds() != null) {
                for (String depRefId : taskReq.getDependsOnReferenceIds()) {
                    Task depTask = taskMap.get(depRefId);
                    if (depTask == null) {
                        throw new IllegalArgumentException("Dependency reference ID not found: " + depRefId);
                    }
                    task.getDependencies().add(depTask);
                    depTask.getDependentTasks().add(task);
                }
            }
            // If task has no dependencies, it is READY to execute immediately
            if (task.getDependencies().isEmpty()) {
                task.setStatus(TaskStatus.READY);
            }
        }

        workflow = workflowRepository.save(workflow);
        
        // Publish READY tasks to Kafka (root nodes)
        for (Task task : workflow.getTasks()) {
            if (task.getStatus() == TaskStatus.READY) {
                kafkaProducerService.publishTaskReady(task);
            }
        }
        
        return mapToResponse(workflow);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "workflow", key = "#id")
    public WorkflowResponse getWorkflow(UUID id, User user) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));
        
        if (!workflow.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        
        return mapToResponse(workflow);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "workflows", key = "#user.id")
    public List<WorkflowResponse> getUserWorkflows(User user) {
        return workflowRepository.findByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WorkflowResponse mapToResponse(Workflow workflow) {
        return WorkflowResponse.builder()
                .id(workflow.getId())
                .userId(workflow.getUser().getId())
                .name(workflow.getName())
                .status(workflow.getStatus())
                .createdAt(workflow.getCreatedAt())
                .startedAt(workflow.getStartedAt())
                .completedAt(workflow.getCompletedAt())
                .tasks(workflow.getTasks().stream().map(this::mapTaskToResponse).collect(Collectors.toList()))
                .build();
    }

    private TaskResponse mapTaskToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .workflowId(task.getWorkflow().getId())
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .priority(task.getPriority())
                .payload(task.getPayload())
                .result(task.getResult())
                .retryCount(task.getRetryCount())
                .maxRetries(task.getMaxRetries())
                .timeoutSeconds(task.getTimeoutSeconds())
                .dependencyIds(task.getDependencies().stream().map(Task::getId).collect(Collectors.toList()))
                .createdAt(task.getCreatedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
