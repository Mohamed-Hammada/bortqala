package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.domain.PayrollRunLine;
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

    public PayrollExecutionService(PayrollRunHeaderRepository runHeaderRepository,
                                   PayrollRunLineRepository runLineRepository) {
        this.runHeaderRepository = runHeaderRepository;
        this.runLineRepository = runLineRepository;
    }

    @Transactional
    public PayrollRunHeader createRun(String runNumber, String periodId, LocalDate runDate) {
        PayrollRunHeader run = new PayrollRunHeader(runNumber, periodId, runDate);
        return runHeaderRepository.save(run);
    }

    @Transactional
    public PayrollRunLine addRunLine(String runId, String employeeId, BigDecimal basicSalary, BigDecimal allowances, BigDecimal deductions) {
        PayrollRunLine line = new PayrollRunLine(runId, employeeId, basicSalary, allowances, deductions);
        return runLineRepository.save(line);
    }

    @Transactional
    public PayrollRunHeader calculateRun(String runId) {
        PayrollRunHeader run = getRun(runId);
        List<PayrollRunLine> lines = runLineRepository.findByRunId(runId);

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
}
