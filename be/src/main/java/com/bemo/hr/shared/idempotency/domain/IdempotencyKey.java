package com.bemo.hr.shared.idempotency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "operation_type", nullable = false, length = 60)
    private String operationType;
    @Column(name = "operation_id", nullable = false, length = 80)
    private String operationId;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "response_reference_or_body", length = 4000)
    private String responseReferenceOrBody;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "completed_at")
    private Long completedAt;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String operationType, String operationId, String requestHash) {
        this.id = UUID.randomUUID().toString();
        this.operationType = operationType;
        this.operationId = operationId;
        this.requestHash = requestHash;
        this.status = STATUS_IN_PROGRESS;
    }

    public void complete(String responseReferenceOrBody) {
        this.status = STATUS_COMPLETED;
        this.responseReferenceOrBody = responseReferenceOrBody;
        this.completedAt = System.currentTimeMillis();
    }

    public void fail() {
        this.status = STATUS_FAILED;
        this.completedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getOperationType() { return operationType; }
    public String getOperationId() { return operationId; }
    public String getRequestHash() { return requestHash; }
    public String getStatus() { return status; }
    public String getResponseReferenceOrBody() { return responseReferenceOrBody; }
    public long getCreatedAt() { return createdAt; }
    public Long getCompletedAt() { return completedAt; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }
}
