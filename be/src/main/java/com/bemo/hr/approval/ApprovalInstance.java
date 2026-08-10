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
    @Column(name = "workflow_definition_version", nullable = false) private int workflowDefinitionVersion;
    @Column(name = "document_snapshot_json", nullable = false, length = 4000) private String documentSnapshotJson;
    @Column(name = "step_due_at") private Instant stepDueAt;
    @Column(name = "escalated_at") private Instant escalatedAt;
    @Column(name = "escalation_level", nullable = false) private int escalationLevel;
    @Version private Long version;

    protected ApprovalInstance() { }

    public ApprovalInstance(String workflowDefinitionId, int workflowDefinitionVersion, String documentType,
                            String documentId, String submittedBy, String documentSnapshotJson, Integer escalationHours) {
        this.id = UUID.randomUUID().toString();
        this.workflowDefinitionId = workflowDefinitionId;
        this.documentType = documentType.strip().toUpperCase();
        this.documentId = documentId;
        this.currentStepOrder = 1;
        this.status = "SUBMITTED";
        this.submittedBy = submittedBy;
        this.workflowDefinitionVersion = workflowDefinitionVersion;
        this.documentSnapshotJson = documentSnapshotJson == null || documentSnapshotJson.isBlank() ? "{}" : documentSnapshotJson;
        setDue(escalationHours);
    }

    public ApprovalInstance(String workflowDefinitionId, String documentType, String documentId, String submittedBy) {
        this(workflowDefinitionId, 1, documentType, documentId, submittedBy, "{}", null);
    }

    public void advanceStep(int nextStepOrder, Integer escalationHours) {
        this.currentStepOrder = nextStepOrder;
        this.status = "UNDER_REVIEW";
        this.escalatedAt = null; this.escalationLevel = 0; setDue(escalationHours);
    }
    public void advanceStep(int nextStepOrder) { advanceStep(nextStepOrder, null); }

    public void approve() {
        this.status = "APPROVED";
        this.completedAt = Instant.now();
    }

    public void reject() {
        this.status = "REJECTED";
        this.completedAt = Instant.now();
    }

    public boolean isOverdue(Instant now) { return stepDueAt != null && now.isAfter(stepDueAt) && completedAt == null; }
    public void escalate(Instant now) { if (isOverdue(now)) { escalationLevel++; escalatedAt = now; } }
    private void setDue(Integer hours) { stepDueAt = hours == null || hours <= 0 ? null : Instant.now().plusSeconds(hours * 3600L); }

    @PrePersist void prePersist() { submittedAt = Instant.now(); }
}
