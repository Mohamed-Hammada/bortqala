package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class PayrollSnapshotService {

    private final PayrollInputSnapshotRepository snapshotRepository;

    public PayrollSnapshotService(PayrollInputSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public PayrollInputSnapshot captureSnapshot(CalculationInputs input, String username) {
        BigDecimal hourlyRate = input.baseSalary().signum() <= 0 ? BigDecimal.ZERO
                : input.baseSalary().divide(input.workingHourDivisor(), 8, RoundingMode.HALF_UP);
        BigDecimal overtime = hourlyRate.multiply(BigDecimal.valueOf(input.overtimeMinutes()))
                .multiply(input.overtimeMultiplier()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal lateness = hourlyRate.multiply(BigDecimal.valueOf(input.lateMinutes()))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal deductions = lateness.add(input.otherDeductions());
        BigDecimal allowances = overtime.add(input.otherBonuses());
        BigDecimal gross = input.baseSalary().add(allowances);
        BigDecimal net = gross.subtract(deductions).subtract(input.advanceDeduction()).max(BigDecimal.ZERO);
        return snapshotRepository.findByPayrollRunIdAndEmployeeId(input.payrollRunId(), input.employeeId())
                .orElseGet(() -> snapshotRepository.save(new PayrollInputSnapshot(
                        input.payrollRunId(), input.employeeId(), input.periodId(), input.periodStart(), input.periodEnd(), input.baseSalary(),
                        input.workedMinutes(), input.overtimeMinutes(), input.lateMinutes(), input.absenceDays(),
                        input.payrollPolicyId(), input.payrollPolicyVersion(), input.workingHourDivisor(),
                        input.overtimeMultiplier(), deductions, allowances, input.advanceBalance(),
                        input.advanceDeduction(), gross, net, username
                )));
    }

    @Transactional(readOnly = true)
    public Optional<PayrollInputSnapshot> find(String payrollRunId, String employeeId) {
        if (payrollRunId == null || payrollRunId.isBlank()) return Optional.empty();
        return snapshotRepository.findByPayrollRunIdAndEmployeeId(payrollRunId, employeeId);
    }

    @Transactional(readOnly = true)
    public Optional<PayrollInputSnapshot> findById(String snapshotId) {
        if (snapshotId == null || snapshotId.isBlank()) return Optional.empty();
        return snapshotRepository.findById(snapshotId);
    }

    public record CalculationInputs(
            String payrollRunId, String employeeId, String periodId, LocalDate periodStart, LocalDate periodEnd,
            BigDecimal baseSalary, long workedMinutes, long overtimeMinutes, long lateMinutes, int absenceDays,
            String payrollPolicyId, long payrollPolicyVersion, BigDecimal workingHourDivisor,
            BigDecimal overtimeMultiplier, BigDecimal otherDeductions, BigDecimal otherBonuses,
            BigDecimal advanceBalance, BigDecimal advanceDeduction
    ) { }
}
