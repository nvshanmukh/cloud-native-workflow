package com.example.workflow.worker;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WorkerRegistry {

    private final Map<String, TaskExecutor> executorMap;

    public WorkerRegistry(List<TaskExecutor> executors) {
        this.executorMap = executors.stream()
                .collect(Collectors.toMap(TaskExecutor::getTaskType, Function.identity()));
    }

    public TaskExecutor getExecutor(String taskType) {
        TaskExecutor executor = executorMap.get(taskType);
        if (executor == null) {
            throw new IllegalArgumentException("No executor found for task type: " + taskType);
        }
        return executor;
    }
}
