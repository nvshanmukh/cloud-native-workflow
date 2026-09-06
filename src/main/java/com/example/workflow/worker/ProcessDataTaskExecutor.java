package com.example.workflow.worker;

import com.example.workflow.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProcessDataTaskExecutor implements TaskExecutor {

    @Override
    public String getTaskType() {
        return "PROCESS_DATA";
    }

    @Override
    public String execute(Task task) throws Exception {
        log.info("Executing PROCESS_DATA for task id: {}", task.getId());
        
        // Simulate CPU bound work
        Thread.sleep(2000);
        
        return "{\"status\": \"success\", \"processedRecords\": 500}";
    }
}
