package com.example.workflow.entity;

public enum TaskStatus {
    PENDING,
    READY,
    RUNNING,
    COMPLETED,
    RETRYING,
    FAILED,
    DEAD_LETTER
}
