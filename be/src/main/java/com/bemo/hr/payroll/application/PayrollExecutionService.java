package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.domain.PayrollRunLine;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunHeaderRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunLineRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollExecutionService {

    private final PayrollRunHeaderRepository runHeaderRepository;
    private final PayrollRunLineRepository runLineRepository;
    private final PayrollInputSnapshotRepository snapshotRepository;

    public PayrollExecutionService(PayrollRunHeaderRepository runHeaderRepository,
                                   PayrollRunLineRepository runLineRepository,
                                   PayrollInputSnapshotRepository snapshotRepository) {
        this.runHeaderRepository = runHeaderRepository;
        this.runLineRepository = runLineRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public PayrollRunHeader createRun(String runNumber, String periodId, LocalDate runDate) {
        PayrollRunHeader run = new PayrollRunHeader(runNumber, periodId, runDate);
        return runHeaderRepository.save(run);
    }

    @Transactional
    public PayrollRunLine addRunLine(String runId, String employeeId, BigDecimal basicSalary, BigDecimal allowances, BigDecimal deductions) {
        getRunForUpdate(runId);
        throw new BusinessRuleException(
                "Manual payroll inputs are disabled; calculate the register so inputs are frozen from source evidence.",
                "PAYROLL_MANUAL_RUN_LINES_DISABLED", HttpStatus.CONFLICT);
    }

    @Transactional
    public PayrollRunHeader calculateRun(String runId) {
        PayrollRunHeader run = getRunForUpdate(runId);
        if (run.getStatus() != PayrollRunHeader.Status.DRAFT && run.getStatus() != PayrollRunHeader.Status.CALCULATED) {
            throw new BusinessRuleException("Approved or posted payroll runs cannot be recalculated", "PAYROLL_RUN_FROZEN", HttpStatus.CONFLICT);
        }
        List<PayrollRunLine> lines = runLineRepository.findByRunId(runId);

        List<PayrollInputSnapshot> snapshots = snapshotRepository.findByPayrollRunId(runId);
        if (snapshots.isEmpty()) {
            throw new BusinessRuleException("The payroll run has no frozen input snapshots",
                    "PAYROLL_RUN_SNAPSHOTS_REQUIRED", HttpStatus.CONFLICT);
        }
        lines = snapshots.stream().map(s -> new PayrollRunLine(runId, s.getEmployeeId(), s.getId(),
                s.getBaseSalary(), s.getAllowanceAmount(), s.getDeductionAmount().add(s.getAdvanceDeduction()))).toList();

        BigDecimal gross = lines.stream().map(l -> l.getBasicSalary().add(l.getAllowances())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deductions = lines.stream().map(PayrollRunLine::getDeductions).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = lines.stream().map(PayrollRunLine::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);

        run.updateTotals(gross, deductions, net);
        return runHeaderRepository.save(run);
    }

    @Transactional
    public PayrollRunHeader approveRun(String runId) {
        PayrollRunHeader run = getRun(runId);
        run.approve();
        return runHeaderRepository.save(run);
    }

    @Transactional
    public PayrollRunHeader postRun(String runId) {
        PayrollRunHeader run = getRun(runId);
        run.post();
        return runHeaderRepository.save(run);
    }

    private PayrollRunHeader getRun(String id) {
        return runHeaderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll run not found", "PAYROLL_RUN_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private PayrollRunHeader getRunForUpdate(String id) {
        return runHeaderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll run not found", "PAYROLL_RUN_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
