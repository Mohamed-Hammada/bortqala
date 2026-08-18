package com.bemo.hr.payroll.application;

import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import com.bemo.hr.payroll.domain.PayrollGlPosting;
import com.bemo.hr.payroll.domain.PayrollRunHeader;
import com.bemo.hr.payroll.infrastructure.PayrollGlPostingRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
public class PayrollGlPostingService {
    public static final String PAYROLL_ACCRUAL_EVENT = "PAYROLL_ACCRUAL";

    private final PayrollGlPostingRepository payrollGlPostingRepository;
    private final SubledgerPostingService subledgerPostingService;

    public PayrollGlPostingService(PayrollGlPostingRepository payrollGlPostingRepository,
                                   SubledgerPostingService subledgerPostingService) {
        this.payrollGlPostingRepository = payrollGlPostingRepository;
        this.subledgerPostingService = subledgerPostingService;
    }

    @Transactional
    public PayrollGlPosting postApprovedRun(PayrollRunHeader run, String actor) {
        log.debug("postApprovedRun called with runId={}, actor={}", run.getId(), actor);
        PayrollGlPosting replay = payrollGlPostingRepository.findByPayrollPeriodId(run.getPeriodId()).orElse(null);
        if (replay != null) {
            log.debug("postApprovedRun returning existing posting id={} for periodId={}", replay.getId(), run.getPeriodId());
            return replay;
        }
        if (run.getStatus() != PayrollRunHeader.Status.APPROVED) {
            log.warn("Validation failed: Payroll GL posting requires an approved payroll run runId={}", run.getId());
            throw new BusinessRuleException("Payroll GL posting requires an approved payroll run.",
                    "PAYROLL_STATE_TRANSITION_INVALID", HttpStatus.CONFLICT);
        }
        if (run.getTotalGross() == null || run.getTotalGross().signum() <= 0) {
            log.warn("Validation failed: Payroll register has no positive gross amount runId={}", run.getId());
            throw new BusinessRuleException("Payroll register has no positive gross amount to post.",
                    "PAYROLL_REGISTER_EMPTY", HttpStatus.CONFLICT);
        }
        String safeActor = actor == null || actor.isBlank() ? "SYSTEM" : actor;
        try {
            var journalEntry = subledgerPostingService.postProfileEvent(
                    "PAYROLL", "PAYROLL_RUN", run.getId(), PAYROLL_ACCRUAL_EVENT,
                    "PAYROLL-ACCRUAL:" + run.getId(), run.getRunDate(), "Payroll accrual " + run.getRunNumber(),
                    Map.of("TOTAL_GROSS", run.getTotalGross(), "TOTAL_NET", run.getTotalNet(),
                            "TOTAL_DEDUCTIONS", run.getTotalDeductions()), null, null, null, safeActor);
            PayrollGlPosting saved = payrollGlPostingRepository.save(new PayrollGlPosting(
                    run.getPeriodId(), journalEntry.getId(), run.getTotalGross(), run.getTotalNet()));
            log.info("PayrollGlPosting created id={} periodId={}", saved.getId(), run.getPeriodId());
            return saved;
        } catch (Exception e) {
            log.error("postApprovedRun failed for runId={}", run.getId(), e);
            throw e;
        }
    }

    @Deprecated
    @Transactional
    public PayrollGlPosting postPayrollToGl(String payrollPeriodId, String journalId,
                                            BigDecimal grossAmount, BigDecimal netAmount) {
        throw new BusinessRuleException("Payroll GL posting is server-managed through the payroll lifecycle.",
                "PAYROLL_GL_POSTING_SERVER_MANAGED", HttpStatus.CONFLICT);
    }

    @Transactional(readOnly = true)
    public PayrollGlPosting getGlPosting(String payrollPeriodId) {
        log.debug("getGlPosting called with payrollPeriodId={}", payrollPeriodId);
        return payrollGlPostingRepository.findByPayrollPeriodId(payrollPeriodId)
                .orElseThrow(() -> new BusinessRuleException("Payroll GL posting not found",
                        "PAYROLL_GL_POSTING_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
