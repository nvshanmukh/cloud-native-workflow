package com.example.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowCreateRequest {
    
    @NotBlank
    private String name;
    
    @NotEmpty
    @Valid
    private List<TaskCreateRequest> tasks;
}
