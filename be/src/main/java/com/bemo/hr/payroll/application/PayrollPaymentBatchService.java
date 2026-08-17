package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollPaymentBatch;
import com.bemo.hr.payroll.infrastructure.PayrollPaymentBatchRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class PayrollPaymentBatchService {

    private final PayrollPaymentBatchRepository repository;

    public PayrollPaymentBatchService(PayrollPaymentBatchRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PayrollPaymentBatch createBatch(String payrollPeriodId, BigDecimal totalAmount, int employeeCount) {
        log.debug("createBatch called with payrollPeriodId={}, totalAmount={}, employeeCount={}", payrollPeriodId, totalAmount, employeeCount);
        PayrollPaymentBatch batch = new PayrollPaymentBatch(payrollPeriodId, totalAmount, employeeCount);
        PayrollPaymentBatch saved = repository.save(batch);
        log.info("PayrollPaymentBatch created id={}", saved.getId());
        return saved;
    }

    @Transactional
    public PayrollPaymentBatch processBatch(String id) {
        log.debug("processBatch called with id={}", id);
        PayrollPaymentBatch batch = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll payment batch not found", "PAYROLL_BATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
        batch.process();
        PayrollPaymentBatch saved = repository.save(batch);
        log.info("PayrollPaymentBatch processed id={}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PayrollPaymentBatch> getBatchesForPeriod(String payrollPeriodId) {
        return repository.findByPayrollPeriodId(payrollPeriodId);
    }
}
