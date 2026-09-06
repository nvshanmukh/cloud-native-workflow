package com.example.workflow.dto;

import com.example.workflow.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class TaskCreateRequest {
    // A local reference ID provided by the client (e.g., "taskA") to wire dependencies before UUIDs are generated
    @NotBlank
    private String referenceId;
    
    @NotBlank
    private String taskType;
    
    private String payload;
    
    private TaskPriority priority = TaskPriority.MEDIUM;
    
    private int maxRetries = 3;
    
    private int timeoutSeconds = 300;
    
    private List<String> dependsOnReferenceIds;
}
