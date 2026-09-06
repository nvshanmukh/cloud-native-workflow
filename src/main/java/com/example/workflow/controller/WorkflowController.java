package com.example.workflow.controller;

import com.example.workflow.dto.WorkflowCreateRequest;
import com.example.workflow.dto.WorkflowResponse;
import com.example.workflow.entity.User;
import com.example.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody WorkflowCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(workflowService.createWorkflow(request, user));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getWorkflows(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(workflowService.getUserWorkflows(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflow(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(workflowService.getWorkflow(id, user));
    }
}
