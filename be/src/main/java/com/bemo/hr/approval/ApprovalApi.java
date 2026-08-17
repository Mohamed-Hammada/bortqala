package com.bemo.hr.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class ApprovalApi {
    private ApprovalApi() {
    }

    public record StepRequest(
            int stepOrder,
            @NotBlank String stepCode,
            @NotBlank String name,
            String requiredRole,
            String requiredUserId,
            BigDecimal amountFrom,
            BigDecimal amountTo,
            int minimumApprovals,
            boolean allowSelfApproval,
            Integer escalationHours,
            String decisionPolicy
    ) {
        public StepRequest(int stepOrder, String stepCode, String name, String requiredRole, String requiredUserId,
                           BigDecimal amountFrom, BigDecimal amountTo, int minimumApprovals, boolean allowSelfApproval,
                           Integer escalationHours) {
            this(stepOrder, stepCode, name, requiredRole, requiredUserId, amountFrom, amountTo,
                    minimumApprovals, allowSelfApproval, escalationHours, "ANY_N");
        }
    }

    public record StepResponse(
            String id,
            int stepOrder,
            String stepCode,
            String name,
            String requiredRole,
            String requiredUserId,
            BigDecimal amountFrom,
            BigDecimal amountTo,
            int minimumApprovals,
            boolean allowSelfApproval,
            Integer escalationHours,
            String decisionPolicy
    ) {
    }

    public record WorkflowDefinitionRequest(
            @NotBlank String documentType,
            @NotBlank String name,
            boolean active,
            List<StepRequest> steps
    ) {
    }

    public record WorkflowDefinitionResponse(
            String id,
            String documentType,
            String name,
            boolean active,
            int version,
            List<StepResponse> steps,
            long createdAt,
            long updatedAt
    ) {
    }

    public record SubmitDocumentRequest(
            @NotBlank String documentType,
            @NotBlank String documentId,
            BigDecimal amount,
            String snapshotJson
    ) {
        public SubmitDocumentRequest(String documentType, String documentId, BigDecimal amount) {
            this(documentType, documentId, amount, "{}");
        }
    }

    public record DecisionRequest(
            @NotBlank String instanceId,
            String comment
    ) {
    }

    public record ApprovalTaskResponse(
            String instanceId,
            String documentType,
            String documentId,
            int currentStepOrder,
            String stepName,
            String requiredRole,
            String status,
            String submittedBy,
            long submittedAt,
            Long dueAt,
            boolean overdue,
            int escalationLevel,
            int approvalsReceived,
            int approvalsRequired,
            String delegatedFrom
    ) {
    }

    public record DecisionResponse(
            String id,
            String instanceId,
            String stepId,
            String decision,
            String comment,
            String decidedBy,
            long decidedAt,
            String delegatedFrom
    ) {
    }

    public record ApprovalInstanceDetailResponse(
            String instanceId,
            String documentType,
            String documentId,
            int currentStepOrder,
            String status,
            String submittedBy,
            long submittedAt,
            Long completedAt,
            int workflowDefinitionVersion,
            String documentSnapshotJson,
            Long stepDueAt,
            boolean overdue,
            int escalationLevel,
            int approvalsReceived,
            int approvalsRequired,
            List<DecisionResponse> history
    ) {
    }

    public record DelegationRequest(@NotBlank String delegatorUserId, @NotBlank String delegateUserId,
                                    String documentType, @NotNull Long startsAt, @NotNull Long endsAt,
                                    @NotBlank String reason) {
    }

    public record DelegationResponse(String id, String delegatorUserId, String delegateUserId, String documentType,
                                     long startsAt, long endsAt, String reason, boolean active, String createdBy,
                                     long createdAt, long version) {
    }

    public record ReassignRequest(@NotBlank String userId, @NotBlank String reason) {
    }
}
