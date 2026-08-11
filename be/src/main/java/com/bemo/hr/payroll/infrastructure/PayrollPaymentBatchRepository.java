package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollPaymentBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollPaymentBatchRepository extends JpaRepository<PayrollPaymentBatch, String> {
    List<PayrollPaymentBatch> findByPayrollPeriodId(String payrollPeriodId);
}
