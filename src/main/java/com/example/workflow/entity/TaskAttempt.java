package com.example.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAttempt {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    private Instant completedAt;
}
