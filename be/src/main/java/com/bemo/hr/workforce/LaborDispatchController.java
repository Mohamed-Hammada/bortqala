package com.bemo.hr.workforce;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce")
public class LaborDispatchController {

    private final LaborDispatchService laborDispatchService;

    public LaborDispatchController(LaborDispatchService laborDispatchService) {
        this.laborDispatchService = laborDispatchService;
    }

    public record CreateDispatchPayload(String requestId, String contractorId, String dispatchDate) {}
    public record AssignWorkerPayload(String workerId, String requestLineId, String contractorId, String fromDate, String toDate, BigDecimal agreedRate, BigDecimal agreedHours) {}
    public record RejectAssignmentPayload(String reason) {}
    public record ReplaceAssignmentPayload(String newWorkerId, BigDecimal agreedRate, BigDecimal agreedHours) {}

    @PostMapping("/dispatches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public LaborDispatch createDispatch(@RequestBody CreateDispatchPayload payload, Authentication authentication) {
        LocalDate dispatchDate = payload.dispatchDate() == null ? null : LocalDate.parse(payload.dispatchDate());
        return laborDispatchService.createDispatch(payload.requestId(), payload.contractorId(), dispatchDate, authentication.getName());
    }

    @GetMapping("/dispatches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'VIEWER')")
    public List<LaborDispatch> listDispatches() {
        return laborDispatchService.listDispatches();
    }

    @PostMapping("/dispatches/{id}/dispatch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public LaborDispatch dispatch(@PathVariable String id, Authentication authentication) {
        return laborDispatchService.dispatch(id, authentication.getName());
    }

    @PostMapping("/dispatches/{id}/accept")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public LaborDispatch accept(@PathVariable String id, Authentication authentication) {
        return laborDispatchService.accept(id, authentication.getName());
    }

    @PostMapping("/dispatches/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public LaborDispatch cancel(@PathVariable String id, Authentication authentication) {
        return laborDispatchService.cancel(id, authentication.getName());
    }

    @PostMapping("/dispatches/{id}/assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkerAssignment assignWorker(@PathVariable String id, @RequestBody AssignWorkerPayload payload, Authentication authentication) {
        return laborDispatchService.assignWorker(
                id,
                payload.workerId(),
                payload.requestLineId(),
                payload.contractorId(),
                LocalDate.parse(payload.fromDate()),
                LocalDate.parse(payload.toDate()),
                payload.agreedRate(),
                payload.agreedHours(),
                authentication.getName()
        );
    }

    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkerAssignment acceptAssignment(@PathVariable String id, Authentication authentication) {
        return laborDispatchService.acceptAssignment(id, authentication.getName());
    }

    @PostMapping("/assignments/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkerAssignment rejectAssignment(@PathVariable String id, @RequestBody RejectAssignmentPayload payload, Authentication authentication) {
        return laborDispatchService.rejectAssignment(id, payload.reason(), authentication.getName());
    }

    @PostMapping("/assignments/{id}/replace")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER')")
    public WorkerAssignment replaceAssignment(@PathVariable String id, @RequestBody ReplaceAssignmentPayload payload, Authentication authentication) {
        return laborDispatchService.replaceAssignment(id, payload.newWorkerId(), payload.agreedRate(), payload.agreedHours(), authentication.getName());
    }

    @GetMapping("/dispatches/{id}/assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'VIEWER')")
    public List<WorkerAssignment> getAssignments(@PathVariable String id) {
        return laborDispatchService.getAssignmentsByDispatch(id);
    }
}
