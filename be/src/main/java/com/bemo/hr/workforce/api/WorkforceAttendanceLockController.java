package com.bemo.hr.workforce.api;

import com.bemo.hr.workforce.application.WorkforceAttendanceLockService;
import com.bemo.hr.workforce.domain.WorkforceAttendanceLock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workforce/attendance-locks")
public class WorkforceAttendanceLockController {

    private final WorkforceAttendanceLockService lockService;

    public WorkforceAttendanceLockController(WorkforceAttendanceLockService lockService) {
        this.lockService = lockService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'HR_MANAGER')")
    public WorkforceAttendanceLock lockAttendance(@RequestBody LockAttendancePayload payload) {
        return lockService.lockAttendance(payload.contractorId(), payload.periodId(), payload.totalHours(), payload.lockedBy());
    }

    @PostMapping("/{id}/correct")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'HR_MANAGER')")
    public WorkforceAttendanceLock correctLock(@PathVariable String id, @RequestBody CorrectLockPayload payload) {
        return lockService.correctLock(id, payload.newTotalHours(), payload.reason());
    }

    @GetMapping("/contractors/{contractorId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WORKFORCE_MANAGER', 'HR_MANAGER', 'VIEWER')")
    public List<WorkforceAttendanceLock> getLocksForContractor(@PathVariable String contractorId) {
        return lockService.getLocksForContractor(contractorId);
    }

    public record LockAttendancePayload(String contractorId, String periodId, BigDecimal totalHours, String lockedBy) {
    }

    public record CorrectLockPayload(BigDecimal newTotalHours, String reason) {
    }
}
