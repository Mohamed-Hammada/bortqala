package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollGlPosting;
import com.bemo.hr.payroll.infrastructure.PayrollGlPostingRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PayrollGlPostingService {

    private final PayrollGlPostingRepository repository;

    public PayrollGlPostingService(PayrollGlPostingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PayrollGlPosting postPayrollToGl(String payrollPeriodId, String journalId, BigDecimal grossAmount, BigDecimal netAmount) {
        PayrollGlPosting posting = repository.findByPayrollPeriodId(payrollPeriodId)
                .orElseGet(() -> new PayrollGlPosting(payrollPeriodId, journalId, grossAmount, netAmount));
        return repository.save(posting);
    }

    @Transactional(readOnly = true)
    public PayrollGlPosting getGlPosting(String payrollPeriodId) {
        return repository.findByPayrollPeriodId(payrollPeriodId)
                .orElseThrow(() -> new BusinessRuleException("Payroll GL posting not found", "PAYROLL_GL_POSTING_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
