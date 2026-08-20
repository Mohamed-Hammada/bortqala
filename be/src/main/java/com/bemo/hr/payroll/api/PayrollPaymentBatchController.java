package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.PayrollPaymentBatchService;
import com.bemo.hr.payroll.domain.PayrollPaymentBatch;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll/payment-batches")
public class PayrollPaymentBatchController {

    private final PayrollPaymentBatchService batchService;

    public PayrollPaymentBatchController(PayrollPaymentBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public PayrollPaymentBatch createBatch(@RequestBody CreateBatchPayload payload) {
        return batchService.createBatch(payload.payrollPeriodId(), payload.totalAmount(), payload.employeeCount());
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public PayrollPaymentBatch processBatch(@PathVariable String id) {
        return batchService.processBatch(id);
    }

    @GetMapping("/period/{payrollPeriodId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<PayrollPaymentBatch> getBatchesForPeriod(@PathVariable String payrollPeriodId) {
        return batchService.getBatchesForPeriod(payrollPeriodId);
    }

    public record CreateBatchPayload(String payrollPeriodId, BigDecimal totalAmount, int employeeCount) {
    }
}
