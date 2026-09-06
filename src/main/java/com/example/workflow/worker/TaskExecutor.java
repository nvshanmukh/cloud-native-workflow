package com.example.workflow.worker;

import com.example.workflow.entity.Task;

public interface TaskExecutor {
    
    /**
     * @return The task type string this executor handles (e.g., "FETCH_DATA")
     */
    String getTaskType();

    /**
     * Executes the task logic.
     * @param task the task to execute
     * @return the result payload as a string
     * @throws Exception if execution fails
     */
    String execute(Task task) throws Exception;
}
