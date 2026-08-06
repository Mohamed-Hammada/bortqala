package com.bemo.hr.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_instances")
@Getter
public class ApprovalInstance {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "workflow_definition_id", nullable = false, length = 36) private String workflowDefinitionId;
    @Column(name = "document_type", nullable = false, length = 50) private String documentType;
    @Column(name = "document_id", nullable = false, length = 36) private String documentId;
    @Column(name = "current_step_order", nullable = false) private int currentStepOrder;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "submitted_by", nullable = false, length = 160) private String submittedBy;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Version private Long version;

    protected ApprovalInstance() { }

    public ApprovalInstance(String workflowDefinitionId, String documentType, String documentId, String submittedBy) {
        this.id = UUID.randomUUID().toString();
        this.workflowDefinitionId = workflowDefinitionId;
        this.documentType = documentType.strip().toUpperCase();
        this.documentId = documentId;
        this.currentStepOrder = 1;
        this.status = "SUBMITTED";
        this.submittedBy = submittedBy;
    }

    public void advanceStep(int nextStepOrder) {
        this.currentStepOrder = nextStepOrder;
        this.status = "UNDER_REVIEW";
    }

    public void approve() {
        this.status = "APPROVED";
        this.completedAt = Instant.now();
    }

    public void reject() {
        this.status = "REJECTED";
        this.completedAt = Instant.now();
    }

    @PrePersist void prePersist() { submittedAt = Instant.now(); }
}
