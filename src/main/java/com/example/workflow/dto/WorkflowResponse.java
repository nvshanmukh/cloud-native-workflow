package com.example.workflow.dto;

import com.example.workflow.entity.WorkflowStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WorkflowResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private WorkflowStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private List<TaskResponse> tasks;
}
