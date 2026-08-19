package com.bemo.hr.approval;

import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalWorkflowService service;

    @GetMapping("/approval-workflows")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.HR_MANAGER + " or " + Roles.PROCUREMENT_MANAGER + " or " + Roles.WORKFORCE_MANAGER)
    public List<ApprovalApi.WorkflowDefinitionResponse> listDefinitions() {
        return service.listWorkflowDefinitions();
    }

    @GetMapping("/approval-workflows/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.HR_MANAGER + " or " + Roles.PROCUREMENT_MANAGER + " or " + Roles.WORKFORCE_MANAGER)
    public ApprovalApi.WorkflowDefinitionResponse getDefinition(@PathVariable String id) {
        return service.getWorkflowDefinition(id);
    }

    @PostMapping("/approval-workflows")
    @PreAuthorize(Roles.ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalApi.WorkflowDefinitionResponse createDefinition(@Valid @RequestBody ApprovalApi.WorkflowDefinitionRequest request) {
        return service.createWorkflowDefinition(request);
    }

    @PutMapping("/approval-workflows/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY)
    public ApprovalApi.WorkflowDefinitionResponse updateDefinition(@PathVariable String id,
                                                                   @Valid @RequestBody ApprovalApi.WorkflowDefinitionRequest request) {
        return service.updateWorkflowDefinition(id, request);
    }

    @PostMapping("/approvals/submit")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.ACCOUNTANT + " or " + Roles.FINANCE_MANAGER + " or " + Roles.HR_MANAGER + " or " + Roles.PROCUREMENT_MANAGER + " or " + Roles.PROCUREMENT_USER + " or " + Roles.WORKFORCE_MANAGER + " or " + Roles.WORKFORCE_REVIEWER)
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
    @PreAuthorize(Roles.ADMIN_ONLY)
    public ApprovalApi.ApprovalInstanceDetailResponse reassign(@PathVariable String instanceId,
                                                               @Valid @RequestBody ApprovalApi.ReassignRequest request) {
        return service.reassign(instanceId, request);
    }

    @PostMapping("/approvals/escalate-overdue")
    @PreAuthorize(Roles.ADMIN_ONLY)
    public int escalateOverdue() {
        return service.escalateOverdue();
    }
}
