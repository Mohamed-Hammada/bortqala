package com.bemo.hr.workforce;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce")
public class LaborDispatchController {

    private final LaborDispatchService dispatchService;

    public LaborDispatchController(LaborDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    public record CreateDispatchPayload(String requestId, String contractorId, String dispatchDate) {}
    public record AssignWorkerPayload(String workerId, String requestLineId, String contractorId, String fromDate, String toDate, BigDecimal agreedRate, BigDecimal agreedHours) {}
    public record RejectAssignmentPayload(String reason) {}
    public record ReplaceAssignmentPayload(String newWorkerId, BigDecimal agreedRate, BigDecimal agreedHours) {}

    @PostMapping("/dispatches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public LaborDispatch createDispatch(@RequestBody CreateDispatchPayload payload) {
        return dispatchService.createDispatch(payload.requestId(), payload.contractorId(), LocalDate.parse(payload.dispatchDate()));
    }

    @PostMapping("/dispatches/{id}/assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkerAssignment assignWorker(@PathVariable String id, @RequestBody AssignWorkerPayload payload) {
        return dispatchService.assignWorker(
                id,
                payload.workerId(),
                payload.requestLineId(),
                payload.contractorId(),
                LocalDate.parse(payload.fromDate()),
                LocalDate.parse(payload.toDate()),
                payload.agreedRate(),
                payload.agreedHours()
        );
    }

    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkerAssignment acceptAssignment(@PathVariable String id) {
        return dispatchService.acceptAssignment(id);
    }

    @PostMapping("/assignments/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkerAssignment rejectAssignment(@PathVariable String id, @RequestBody RejectAssignmentPayload payload) {
        return dispatchService.rejectAssignment(id, payload.reason());
    }

    @PostMapping("/assignments/{id}/replace")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkerAssignment replaceAssignment(@PathVariable String id, @RequestBody ReplaceAssignmentPayload payload) {
        return dispatchService.replaceAssignment(id, payload.newWorkerId(), payload.agreedRate(), payload.agreedHours());
    }

    @GetMapping("/dispatches/{id}/assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'VIEWER')")
    public List<WorkerAssignment> getAssignments(@PathVariable String id) {
        return dispatchService.getAssignmentsByDispatch(id);
    }
}
