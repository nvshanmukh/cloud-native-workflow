package com.example.workflow.dto;

import com.example.workflow.entity.TaskPriority;
import com.example.workflow.entity.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TaskResponse {
    private UUID id;
    private UUID workflowId;
    private String taskType;
    private TaskStatus status;
    private TaskPriority priority;
    private String payload;
    private String result;
    private int retryCount;
    private int maxRetries;
    private int timeoutSeconds;
    private List<UUID> dependencyIds;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
}
