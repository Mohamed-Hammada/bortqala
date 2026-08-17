package com.bemo.hr.approval;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_decisions")
@Getter
public class ApprovalDecision {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "instance_id", nullable = false, length = 36)
    private String instanceId;
    @Column(name = "step_id", nullable = false, length = 36)
    private String stepId;
    @Column(nullable = false, length = 30)
    private String decision;
    @Column(length = 1000)
    private String comment;
    @Column(name = "decided_by", nullable = false, length = 160)
    private String decidedBy;
    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;
    @Column(name = "delegated_from", length = 160)
    private String delegatedFrom;

    protected ApprovalDecision() {
    }

    public ApprovalDecision(String instanceId, String stepId, String decision, String comment, String decidedBy, String delegatedFrom) {
        this.id = UUID.randomUUID().toString();
        this.instanceId = instanceId;
        this.stepId = stepId;
        this.decision = decision.strip().toUpperCase();
        this.comment = comment;
        this.decidedBy = decidedBy;
        this.delegatedFrom = delegatedFrom;
    }

    @PrePersist
    void prePersist() {
        decidedAt = Instant.now();
    }
}
