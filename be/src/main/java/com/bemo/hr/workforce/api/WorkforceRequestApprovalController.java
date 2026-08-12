package com.bemo.hr.workforce.api;

import com.bemo.hr.workforce.application.WorkforceRequestApprovalService;
import com.bemo.hr.workforce.domain.WorkforceRequestApproval;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/requests/approvals")
public class WorkforceRequestApprovalController {

    private final WorkforceRequestApprovalService approvalService;

    public WorkforceRequestApprovalController(WorkforceRequestApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    public record SubmitDecisionPayload(String requestId, String approverUserId, WorkforceRequestApproval.Decision decision, String comment) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'DEPARTMENT_HEAD')")
    public WorkforceRequestApproval submitDecision(@RequestBody SubmitDecisionPayload payload) {
        return approvalService.submitDecision(payload.requestId(), payload.approverUserId(), payload.decision(), payload.comment());
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'VIEWER')")
    public List<WorkforceRequestApproval> getApprovalsForRequest(@PathVariable String requestId) {
        return approvalService.getApprovalsForRequest(requestId);
    }
}
