package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollRetroAdjustment;
import com.bemo.hr.payroll.infrastructure.PayrollRetroAdjustmentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class PayrollRetroAdjustmentService {

    private final PayrollRetroAdjustmentRepository repository;

    public PayrollRetroAdjustmentService(PayrollRetroAdjustmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PayrollRetroAdjustment createAdjustment(String employeeId, String payrollPeriodId, String adjustmentType, BigDecimal amount, String reason) {
        log.debug("createAdjustment called with employeeId={}, payrollPeriodId={}, adjustmentType={}, amount={}", employeeId, payrollPeriodId, adjustmentType, amount);
        PayrollRetroAdjustment adjustment = new PayrollRetroAdjustment(employeeId, payrollPeriodId, adjustmentType, amount, reason);
        PayrollRetroAdjustment saved = repository.save(adjustment);
        log.info("PayrollRetroAdjustment created id={}", saved.getId());
        return saved;
    }

    @Transactional
    public PayrollRetroAdjustment approveAdjustment(String id) {
        log.debug("approveAdjustment called with id={}", id);
        PayrollRetroAdjustment adjustment = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll retro adjustment not found", "PAYROLL_RETRO_NOT_FOUND", HttpStatus.NOT_FOUND));
        adjustment.approve();
        PayrollRetroAdjustment saved = repository.save(adjustment);
        log.info("PayrollRetroAdjustment approved id={}", saved.getId());
        return saved;
    }

    @Transactional
    public PayrollRetroAdjustment processAdjustment(String id) {
        log.debug("processAdjustment called with id={}", id);
        PayrollRetroAdjustment adjustment = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll retro adjustment not found", "PAYROLL_RETRO_NOT_FOUND", HttpStatus.NOT_FOUND));
        adjustment.process();
        PayrollRetroAdjustment saved = repository.save(adjustment);
        log.info("PayrollRetroAdjustment processed id={}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PayrollRetroAdjustment> getAdjustmentsForEmployee(String employeeId) {
        return repository.findByEmployeeId(employeeId);
    }
}
