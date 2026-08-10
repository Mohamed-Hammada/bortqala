package com.bemo.hr.approval;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalWorkflowService service;

    @GetMapping("/approval-workflows")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER', 'PROCUREMENT_MANAGER', 'HR_MANAGER')")
    public List<ApprovalApi.WorkflowDefinitionResponse> listDefinitions() {
        return service.listWorkflowDefinitions();
    }

    @GetMapping("/approval-workflows/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER', 'PROCUREMENT_MANAGER', 'HR_MANAGER')")
    public ApprovalApi.WorkflowDefinitionResponse getDefinition(@PathVariable String id) {
        return service.getWorkflowDefinition(id);
    }

    @PostMapping("/approval-workflows")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalApi.WorkflowDefinitionResponse createDefinition(@Valid @RequestBody ApprovalApi.WorkflowDefinitionRequest request) {
        return service.createWorkflowDefinition(request);
    }

    @PutMapping("/approval-workflows/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApprovalApi.WorkflowDefinitionResponse updateDefinition(@PathVariable String id,
                                                                   @Valid @RequestBody ApprovalApi.WorkflowDefinitionRequest request) {
        return service.updateWorkflowDefinition(id, request);
    }

    @PostMapping("/approvals/submit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER', 'PROCUREMENT_MANAGER', 'HR_MANAGER', 'WORKFORCE_REVIEWER', 'ACCOUNTANT', 'PROCUREMENT_USER')")
    public ApprovalApi.ApprovalInstanceDetailResponse submitDocument(@Valid @RequestBody ApprovalApi.SubmitDocumentRequest request) {
        return service.submit(request);
    }

    @GetMapping("/approvals/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public List<ApprovalApi.ApprovalTaskResponse> myTasks() {
        return service.myTasks();
    }

    @PostMapping("/approvals/approve")
    @PreAuthorize("isAuthenticated()")
    public ApprovalApi.ApprovalInstanceDetailResponse approveStep(@Valid @RequestBody ApprovalApi.DecisionRequest request) {
        return service.approve(request);
    }

    @PostMapping("/approvals/reject")
    @PreAuthorize("isAuthenticated()")
    public ApprovalApi.ApprovalInstanceDetailResponse rejectStep(@Valid @RequestBody ApprovalApi.DecisionRequest request) {
        return service.reject(request);
    }

    @GetMapping("/approvals/history/{documentType}/{documentId}")
    @PreAuthorize("isAuthenticated()")
    public ApprovalApi.ApprovalInstanceDetailResponse getHistory(@PathVariable String documentType, @PathVariable String documentId) {
        return service.getHistory(documentType, documentId);
    }

    @GetMapping("/approvals/delegations")
    @PreAuthorize("isAuthenticated()")
    public List<ApprovalApi.DelegationResponse> listDelegations() {
        return service.listDelegations();
    }

    @PostMapping("/approvals/delegations")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalApi.DelegationResponse createDelegation(@Valid @RequestBody ApprovalApi.DelegationRequest request) {
        return service.createDelegation(request);
    }

    @DeleteMapping("/approvals/delegations/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateDelegation(@PathVariable String id) {
        service.deactivateDelegation(id);
    }

    @PutMapping("/approvals/{instanceId}/reassign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApprovalApi.ApprovalInstanceDetailResponse reassign(@PathVariable String instanceId,
                                                                @Valid @RequestBody ApprovalApi.ReassignRequest request) {
        return service.reassign(instanceId, request);
    }

    @PostMapping("/approvals/escalate-overdue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public int escalateOverdue() {
        return service.escalateOverdue();
    }
}
