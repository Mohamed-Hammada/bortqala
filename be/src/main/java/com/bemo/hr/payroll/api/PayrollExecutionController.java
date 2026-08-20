package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.PayrollExecutionService;
import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.domain.PayrollRunLine;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/payroll/runs")
public class PayrollExecutionController {

    private final PayrollExecutionService executionService;

    public PayrollExecutionController(PayrollExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_PAYROLL_MANAGER)
    public PayrollRunHeader createRun(@RequestBody CreateRunPayload payload) {
        return executionService.createRun(payload.runNumber(), payload.periodId(), LocalDate.parse(payload.runDate()));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_PAYROLL_MANAGER)
    public PayrollRunLine addRunLine(@PathVariable String id, @RequestBody AddRunLinePayload payload) {
        return executionService.addRunLine(id, payload.employeeId(), payload.basicSalary(), payload.allowances(), payload.deductions());
    }

    @PostMapping("/{id}/calculate")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_PAYROLL_MANAGER)
    public PayrollRunHeader calculateRun(@PathVariable String id) {
        return executionService.calculateRun(id);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_HR_REVIEWER_PAYROLL_MANAGER)
    public PayrollRunHeader reviewRun(@PathVariable String id) {
        return executionService.reviewRun(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.ADMIN_HR_MANAGER_PAYROLL_MANAGER)
    public PayrollRunHeader approveRun(@PathVariable String id) {
        return executionService.approveRun(id);
    }

    @PostMapping("/{id}/post")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public PayrollRunHeader postRun(@PathVariable String id) {
        return executionService.postRun(id);
    }

    public record CreateRunPayload(String runNumber, String periodId, String runDate) {
    }

    public record AddRunLinePayload(String employeeId, BigDecimal basicSalary, BigDecimal allowances,
                                    BigDecimal deductions) {
    }
}
