package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollRetroAdjustment;
import com.bemo.hr.payroll.infrastructure.PayrollRetroAdjustmentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PayrollRetroAdjustmentService {

    private final PayrollRetroAdjustmentRepository repository;

    public PayrollRetroAdjustmentService(PayrollRetroAdjustmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PayrollRetroAdjustment createAdjustment(String employeeId, String payrollPeriodId, String adjustmentType, BigDecimal amount, String reason) {
        PayrollRetroAdjustment adjustment = new PayrollRetroAdjustment(employeeId, payrollPeriodId, adjustmentType, amount, reason);
        return repository.save(adjustment);
    }

    @Transactional
    public PayrollRetroAdjustment approveAdjustment(String id) {
        PayrollRetroAdjustment adjustment = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll retro adjustment not found", "PAYROLL_RETRO_NOT_FOUND", HttpStatus.NOT_FOUND));
        adjustment.approve();
        return repository.save(adjustment);
    }

    @Transactional
    public PayrollRetroAdjustment processAdjustment(String id) {
        PayrollRetroAdjustment adjustment = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll retro adjustment not found", "PAYROLL_RETRO_NOT_FOUND", HttpStatus.NOT_FOUND));
        adjustment.process();
        return repository.save(adjustment);
    }

    @Transactional(readOnly = true)
    public List<PayrollRetroAdjustment> getAdjustmentsForEmployee(String employeeId) {
        return repository.findByEmployeeId(employeeId);
    }
}
