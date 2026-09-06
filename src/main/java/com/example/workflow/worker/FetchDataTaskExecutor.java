package com.example.workflow.worker;

import com.example.workflow.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FetchDataTaskExecutor implements TaskExecutor {

    @Override
    public String getTaskType() {
        return "FETCH_DATA";
    }

    @Override
    public String execute(Task task) throws Exception {
        log.info("Executing FETCH_DATA for task id: {}", task.getId());
        
        // Simulate network/IO work
        Thread.sleep(1000);
        
        return "{\"status\": \"success\", \"bytesFetched\": 1024}";
    }
}
