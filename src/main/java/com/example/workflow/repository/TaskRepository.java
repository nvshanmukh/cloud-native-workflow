package com.example.workflow.repository;

import com.example.workflow.entity.Task;
import com.example.workflow.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByWorkflowId(UUID workflowId);
    List<Task> findByStatus(TaskStatus status);
    
    @Query("SELECT t FROM Task t JOIN t.dependencies d WHERE d.id = :taskId")
    List<Task> findDependentTasks(@Param("taskId") UUID taskId);
}
