package com.bemo.hr.shared.job.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "scheduled_job_execution_records")
public class ScheduledJobExecutionRecord {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;
    @Column(name = "execution_key", nullable = false, length = 100)
    private String executionKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.RUNNING;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @Column(name = "started_at", nullable = false)
    private long startedAt;
    @Column(name = "completed_at")
    private Long completedAt;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected ScheduledJobExecutionRecord() {
    }

    public ScheduledJobExecutionRecord(String jobName, String executionKey) {
        this.id = UUID.randomUUID().toString();
        this.jobName = jobName;
        this.executionKey = executionKey;
        this.status = Status.RUNNING;
        this.startedAt = System.currentTimeMillis();
    }

    public void markCompleted() {
        this.status = Status.COMPLETED;
        this.completedAt = System.currentTimeMillis();
    }

    public void markFailed(String message) {
        this.status = Status.FAILED;
        this.errorMessage = message;
        this.completedAt = System.currentTimeMillis();
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getExecutionKey() {
        return executionKey;
    }

    public Status getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public enum Status {
        RUNNING, COMPLETED, FAILED
    }
}
