package com.example.workflow.controller;

import com.example.workflow.orchestration.OrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
public class AdminTaskController {

    private final OrchestrationService orchestrationService;

    // TEMPORARY: For testing Milestone 4 before Kafka is implemented
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<String> simulateTaskCompletion(
            @PathVariable UUID taskId,
            @RequestBody String result
    ) {
        orchestrationService.handleTaskCompletion(taskId, result);
        return ResponseEntity.ok("Task completion handled");
    }

    // TEMPORARY: For testing Milestone 4 before Kafka is implemented
    @PostMapping("/{taskId}/fail")
    public ResponseEntity<String> simulateTaskFailure(
            @PathVariable UUID taskId,
            @RequestBody String error
    ) {
        orchestrationService.handleTaskFailure(taskId, error);
        return ResponseEntity.ok("Task failure handled");
    }
}
