package com.bemo.hr.shared.outbox;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sys_outbox_events")
public class OutboxEvent {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String appId;

    @Column(name = "event_type", length = 80, nullable = false)
    private String eventType;

    @Column(name = "aggregate_type", length = 80, nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 60, nullable = false)
    private String aggregateId;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "PENDING";

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 5;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String eventType, String aggregateType, String aggregateId, String payloadJson) {
        this.id = UUID.randomUUID().toString();
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null").strip();
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null").strip();
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null").strip();
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson must not be null");
        this.status = "PENDING";
        this.retryCount = 0;
        this.maxRetries = 5;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.status = "PUBLISHED";
        this.processedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.retryCount++;
        this.lastError = errorMessage != null && errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
        if (this.retryCount >= this.maxRetries) {
            this.status = "DEAD_LETTER";
        } else {
            this.status = "FAILED";
        }
        this.processedAt = Instant.now();
    }

    public void retry() {
        this.status = "PENDING";
        this.lastError = null;
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(this.status);
    }

    public boolean isPublished() {
        return "PUBLISHED".equalsIgnoreCase(this.status);
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
