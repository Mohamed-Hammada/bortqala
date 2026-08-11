package com.bemo.hr.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "approval_workflow_steps")
@Getter
public class ApprovalWorkflowStep {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "workflow_definition_id", nullable = false, length = 36) private String workflowDefinitionId;
    @Column(name = "step_order", nullable = false) private int stepOrder;
    @Column(name = "step_code", nullable = false, length = 50) private String stepCode;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "required_role", length = 50) private String requiredRole;
    @Column(name = "required_user_id", length = 36) private String requiredUserId;
    @Column(name = "amount_from", precision = 19, scale = 2) private BigDecimal amountFrom;
    @Column(name = "amount_to", precision = 19, scale = 2) private BigDecimal amountTo;
    @Column(name = "minimum_approvals") private int minimumApprovals;
    @Column(name = "allow_self_approval") private boolean allowSelfApproval;
    @Column(name = "escalation_hours") private Integer escalationHours;
    @Column(name = "decision_policy", nullable = false, length = 20) private String decisionPolicy;

    protected ApprovalWorkflowStep() { }

    public ApprovalWorkflowStep(String workflowDefinitionId, int stepOrder, String stepCode, String name,
                                String requiredRole, String requiredUserId, BigDecimal amountFrom, BigDecimal amountTo,
                                int minimumApprovals, boolean allowSelfApproval, Integer escalationHours, String decisionPolicy) {
        this.id = UUID.randomUUID().toString();
        this.workflowDefinitionId = workflowDefinitionId;
        this.stepOrder = stepOrder;
        this.stepCode = stepCode.strip().toUpperCase();
        this.name = name.strip();
        this.requiredRole = requiredRole != null && !requiredRole.isBlank() ? requiredRole.strip().toUpperCase() : null;
        this.requiredUserId = requiredUserId != null && !requiredUserId.isBlank() ? requiredUserId.strip() : null;
        this.amountFrom = amountFrom;
        this.amountTo = amountTo;
        this.minimumApprovals = minimumApprovals > 0 ? minimumApprovals : 1;
        this.allowSelfApproval = allowSelfApproval;
        this.escalationHours = escalationHours;
        this.decisionPolicy = decisionPolicy == null || decisionPolicy.isBlank() ? "ANY_N" : decisionPolicy.strip().toUpperCase();
    }

    public ApprovalWorkflowStep(String workflowDefinitionId, int stepOrder, String stepCode, String name,
                                String requiredRole, String requiredUserId, BigDecimal amountFrom, BigDecimal amountTo,
                                int minimumApprovals, boolean allowSelfApproval, Integer escalationHours) {
        this(workflowDefinitionId, stepOrder, stepCode, name, requiredRole, requiredUserId, amountFrom, amountTo,
                minimumApprovals, allowSelfApproval, escalationHours, "ANY_N");
    }
}
