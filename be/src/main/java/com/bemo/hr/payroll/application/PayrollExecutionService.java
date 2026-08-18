package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollInputSnapshot;
import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.domain.PayrollRunLine;
import com.bemo.hr.payroll.infrastructure.PayrollInputSnapshotRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunHeaderRepository;
import com.bemo.hr.payroll.infrastructure.PayrollRunLineRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
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
        log.debug("createRun called with runNumber={}, periodId={}, runDate={}", runNumber, periodId, runDate);
        PayrollRunHeader run = new PayrollRunHeader(runNumber, periodId, runDate);
        PayrollRunHeader saved = runHeaderRepository.save(run);
        log.info("PayrollRunHeader created id={}", saved.getId());
        return saved;
    }

    @Transactional
    public PayrollRunLine addRunLine(String runId, String employeeId, BigDecimal basicSalary, BigDecimal allowances, BigDecimal deductions) {
        log.debug("addRunLine called with runId={}, employeeId={}", runId, employeeId);
        getRunForUpdate(runId);
        log.warn("Validation failed: Manual payroll inputs are disabled");
        throw new BusinessRuleException(
                "Manual payroll inputs are disabled; calculate the register so inputs are frozen from source evidence.",
                "PAYROLL_MANUAL_RUN_LINES_DISABLED", HttpStatus.CONFLICT);
    }

    @Transactional
    public PayrollRunHeader calculateRun(String runId) {
        log.debug("calculateRun called with runId={}", runId);
        PayrollRunHeader run = getRunForUpdate(runId);
        if (run.getStatus() != PayrollRunHeader.Status.DRAFT && run.getStatus() != PayrollRunHeader.Status.CALCULATED) {
            log.warn("Validation failed: Approved or posted payroll run cannot be recalculated runId={}", runId);
            throw new BusinessRuleException("Approved or posted payroll runs cannot be recalculated", "PAYROLL_RUN_FROZEN", HttpStatus.CONFLICT);
        }
        List<PayrollRunLine> lines = runLineRepository.findByRunId(runId);

        List<PayrollInputSnapshot> snapshots = snapshotRepository.findByPayrollRunId(runId);
        if (snapshots.isEmpty()) {
            log.warn("Validation failed: Payroll run has no frozen input snapshots runId={}", runId);
            throw new BusinessRuleException("The payroll run has no frozen input snapshots",
                    "PAYROLL_RUN_SNAPSHOTS_REQUIRED", HttpStatus.CONFLICT);
        }
        lines = snapshots.stream().map(s -> new PayrollRunLine(runId, s.getEmployeeId(), s.getId(),
                s.getBaseSalary(), s.getAllowanceAmount(), s.getDeductionAmount().add(s.getAdvanceDeduction()))).toList();

        BigDecimal gross = lines.stream().map(l -> l.getBasicSalary().add(l.getAllowances())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deductions = lines.stream().map(PayrollRunLine::getDeductions).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = lines.stream().map(PayrollRunLine::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);

        run.updateTotals(gross, deductions, net);
        PayrollRunHeader saved = runHeaderRepository.save(run);
        log.info("PayrollRunHeader calculated id={} gross={} net={}", saved.getId(), gross, net);
        return saved;
    }

    @Transactional
    public PayrollRunHeader approveRun(String runId) {
        log.debug("approveRun called with runId={}", runId);
        PayrollRunHeader run = getRunForUpdate(runId);
        run.approve();
        PayrollRunHeader saved = runHeaderRepository.save(run);
        log.info("PayrollRunHeader approved id={}", saved.getId());
        return saved;
    }

    @Transactional
    public PayrollRunHeader postRun(String runId) {
        log.debug("postRun called with runId={}", runId);
        PayrollRunHeader run = getRunForUpdate(runId);
        run.post();
        PayrollRunHeader saved = runHeaderRepository.save(run);
        log.info("PayrollRunHeader posted id={}", saved.getId());
        return saved;
    }

    private PayrollRunHeader getRun(String id) {
        return runHeaderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll run not found", "PAYROLL_RUN_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public PayrollRunHeader reviewRun(String runId) {
        log.debug("reviewRun called with runId={}", runId);
        PayrollRunHeader run = getRunForUpdate(runId);
        run.transitionTo(PayrollRunHeader.Status.REVIEWED);
        PayrollRunHeader saved = runHeaderRepository.save(run);
        log.info("PayrollRunHeader reviewed id={}", saved.getId());
        return saved;
    }

    private PayrollRunHeader getRunForUpdate(String id) {
        return runHeaderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessRuleException("Payroll run not found", "PAYROLL_RUN_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
