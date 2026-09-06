package com.example.workflow.repository;

import com.example.workflow.entity.TaskAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskAttemptRepository extends JpaRepository<TaskAttempt, UUID> {
    List<TaskAttempt> findByTaskIdOrderByAttemptNumberAsc(UUID taskId);
}
