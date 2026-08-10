package com.bemo.hr.approval;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "approval_instance_steps") @Getter
public class ApprovalInstanceStep {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "instance_id", nullable = false, length = 36) private String instanceId;
    @Column(name = "source_step_id", nullable = false, length = 36) private String sourceStepId;
    @Column(name = "step_order", nullable = false) private int stepOrder;
    @Column(name = "step_code", nullable = false, length = 50) private String stepCode;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "required_role", length = 50) private String requiredRole;
    @Column(name = "required_user_id", length = 100) private String requiredUserId;
    @Column(name = "minimum_approvals", nullable = false) private int minimumApprovals;
    @Column(name = "allow_self_approval", nullable = false) private boolean allowSelfApproval;
    @Column(name = "escalation_hours") private Integer escalationHours;
    @Column(name = "decision_policy", nullable = false, length = 20) private String decisionPolicy;
    @Column(name = "reassigned_by", length = 100) private String reassignedBy;
    @Column(name = "reassigned_at") private Instant reassignedAt;
    @Column(name = "reassignment_reason", length = 500) private String reassignmentReason;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ApprovalInstanceStep() { }
    public ApprovalInstanceStep(String instanceId, ApprovalWorkflowStep source) {
        this(instanceId, source, source.getStepOrder());
    }
    public ApprovalInstanceStep(String instanceId, ApprovalWorkflowStep source, int effectiveOrder) {
        id = UUID.randomUUID().toString(); this.instanceId = instanceId; sourceStepId = source.getId();
        stepOrder = effectiveOrder; stepCode = source.getStepCode(); name = source.getName();
        requiredRole = source.getRequiredRole(); requiredUserId = source.getRequiredUserId();
        minimumApprovals = source.getMinimumApprovals(); allowSelfApproval = source.isAllowSelfApproval();
        escalationHours = source.getEscalationHours(); decisionPolicy = source.getDecisionPolicy(); createdAt = Instant.now();
    }
    public void reassign(String userId, String actor, String reason) {
        requiredUserId = userId.strip(); requiredRole = null; reassignedBy = actor; reassignedAt = Instant.now(); reassignmentReason = reason.strip();
    }
}
