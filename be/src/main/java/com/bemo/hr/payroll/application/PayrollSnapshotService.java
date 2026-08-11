package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PayrollSnapshotService {

    private final PayrollInputSnapshotRepository snapshotRepository;

    public PayrollSnapshotService(PayrollInputSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public PayrollInputSnapshot captureSnapshot(
            String employeeId,
            String periodId,
            BigDecimal workedHours,
            BigDecimal overtimeHours,
            int absenceDays,
            BigDecimal deductionAmount,
            BigDecimal allowanceAmount,
            BigDecimal grossPay,
            BigDecimal netPay,
            String username
    ) {
        return snapshotRepository.findByEmployeeIdAndPeriodId(employeeId, periodId)
                .orElseGet(() -> snapshotRepository.save(new PayrollInputSnapshot(
                        employeeId, periodId, workedHours, overtimeHours, absenceDays,
                        deductionAmount, allowanceAmount, grossPay, netPay, username
                )));
    }
}
