package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.PayrollRetroAdjustmentService;
import com.bemo.hr.payroll.domain.PayrollRetroAdjustment;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll/retro-adjustments")
public class PayrollRetroAdjustmentController {

    private final PayrollRetroAdjustmentService adjustmentService;

    public PayrollRetroAdjustmentController(PayrollRetroAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    public PayrollRetroAdjustment createAdjustment(@RequestBody CreateAdjustmentPayload payload) {
        return adjustmentService.createAdjustment(payload.employeeId(), payload.payrollPeriodId(), payload.adjustmentType(), payload.amount(), payload.reason());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    public PayrollRetroAdjustment approveAdjustment(@PathVariable String id) {
        return adjustmentService.approveAdjustment(id);
    }

    @PostMapping("/{id}/process")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.HR_MANAGER)
    public PayrollRetroAdjustment processAdjustment(@PathVariable String id) {
        return adjustmentService.processAdjustment(id);
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER + " or " + Roles.VIEWER)
    public List<PayrollRetroAdjustment> getAdjustmentsForEmployee(@PathVariable String employeeId) {
        return adjustmentService.getAdjustmentsForEmployee(employeeId);
    }

    public record CreateAdjustmentPayload(String employeeId, String payrollPeriodId, String adjustmentType,
                                          BigDecimal amount, String reason) {
    }
}
