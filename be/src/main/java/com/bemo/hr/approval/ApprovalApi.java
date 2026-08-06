package com.bemo.hr.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class ApprovalApi {
    private ApprovalApi() { }

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
        Integer escalationHours
    ) { }

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
        Integer escalationHours
    ) { }

    public record WorkflowDefinitionRequest(
        @NotBlank String documentType,
        @NotBlank String name,
        boolean active,
        List<StepRequest> steps
    ) { }

    public record WorkflowDefinitionResponse(
        String id,
        String documentType,
        String name,
        boolean active,
        int version,
        List<StepResponse> steps,
        long createdAt,
        long updatedAt
    ) { }

    public record SubmitDocumentRequest(
        @NotBlank String documentType,
        @NotBlank String documentId,
        BigDecimal amount
    ) { }

    public record DecisionRequest(
        @NotBlank String instanceId,
        String comment
    ) { }

    public record ApprovalTaskResponse(
        String instanceId,
        String documentType,
        String documentId,
        int currentStepOrder,
        String stepName,
        String requiredRole,
        String status,
        String submittedBy,
        long submittedAt
    ) { }

    public record DecisionResponse(
        String id,
        String instanceId,
        String stepId,
        String decision,
        String comment,
        String decidedBy,
        long decidedAt
    ) { }

    public record ApprovalInstanceDetailResponse(
        String instanceId,
        String documentType,
        String documentId,
        int currentStepOrder,
        String status,
        String submittedBy,
        long submittedAt,
        Long completedAt,
        List<DecisionResponse> history
    ) { }
}
