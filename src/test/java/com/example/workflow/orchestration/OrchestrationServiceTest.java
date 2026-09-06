package com.example.workflow.orchestration;

import com.example.workflow.entity.Task;
import com.example.workflow.entity.TaskStatus;
import com.example.workflow.entity.Workflow;
import com.example.workflow.kafka.producer.KafkaProducerService;
import com.example.workflow.repository.TaskAttemptRepository;
import com.example.workflow.repository.TaskRepository;
import com.example.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private TaskAttemptRepository taskAttemptRepository;
    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private OrchestrationService orchestrationService;

    private Task task;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setTasks(Collections.emptyList());

        task = new Task();
        task.setId(UUID.randomUUID());
        task.setStatus(TaskStatus.RUNNING);
        task.setWorkflow(workflow);
    }

    @Test
    void testHandleTaskCompletion_Success() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.findDependentTasks(task.getId())).thenReturn(Collections.emptyList());
        when(workflowRepository.findById(workflow.getId())).thenReturn(Optional.of(workflow));

        orchestrationService.handleTaskCompletion(task.getId(), "result");

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals("result", task.getResult());
        verify(taskRepository, times(1)).save(task);
        verify(kafkaProducerService, never()).publishTaskReady(any());
    }

    @Test
    void testHandleTaskCompletion_Idempotent() {
        task.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        orchestrationService.handleTaskCompletion(task.getId(), "result");

        verify(taskRepository, never()).save(any());
    }
}
