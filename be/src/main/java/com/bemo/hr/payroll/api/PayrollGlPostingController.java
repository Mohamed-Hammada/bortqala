package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.PayrollGlPostingService;
import com.bemo.hr.payroll.domain.PayrollGlPosting;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payroll/gl-postings")
public class PayrollGlPostingController {

    private final PayrollGlPostingService glPostingService;

    public PayrollGlPostingController(PayrollGlPostingService glPostingService) {
        this.glPostingService = glPostingService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public PayrollGlPosting postPayrollToGl(@RequestBody PostPayrollToGlPayload payload) {
        return glPostingService.postPayrollToGl(payload.payrollPeriodId(), payload.journalId(), payload.grossAmount(), payload.netAmount());
    }

    @GetMapping("/{payrollPeriodId}")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_HR_MANAGER_VIEWER)
    public PayrollGlPosting getGlPosting(@PathVariable String payrollPeriodId) {
        return glPostingService.getGlPosting(payrollPeriodId);
    }

    public record PostPayrollToGlPayload(String payrollPeriodId, String journalId, BigDecimal grossAmount,
                                         BigDecimal netAmount) {
    }
}
