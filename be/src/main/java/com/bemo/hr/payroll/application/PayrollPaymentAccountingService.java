package com.bemo.hr.payroll.application;

import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import com.bemo.hr.payroll.domain.PaymentMethod;
import com.bemo.hr.payroll.domain.PaymentStatus;
import com.bemo.hr.payroll.domain.SalaryPayment;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
public class PayrollPaymentAccountingService {

    private final SubledgerPostingService subledgerPostingService;

    public PayrollPaymentAccountingService(SubledgerPostingService subledgerPostingService) {
        this.subledgerPostingService = subledgerPostingService;
    }

    @Transactional
    public JournalEntry postDisbursement(SalaryPayment payment, String actor) {
        log.debug("postDisbursement called with paymentId={}, actor={}", payment.getId(), actor);
        if (payment.getPaymentStatus() != PaymentStatus.PAID || payment.getPaidAt() == null) {
            log.warn("Validation failed: Payroll disbursement requires a paid salary row and payment date paymentId={}", payment.getId());
            throw new BusinessRuleException(
                    "Payroll disbursement accounting requires a paid salary row and payment date.",
                    "PAYROLL_PAYMENT_STATE_INVALID",
                    HttpStatus.CONFLICT);
        }
        PaymentMethod method = payment.getPaymentMethod() == null ? PaymentMethod.CASH : payment.getPaymentMethod();
        LocalDate date = payment.getPaidAt().atZone(ZoneOffset.UTC).toLocalDate();
        try {
            JournalEntry journal = subledgerPostingService.postSubledgerEvent(
                    "PAYROLL",
                    "SALARY_PAYMENT",
                    payment.getId(),
                    "PAYROLL_DISBURSEMENT_" + method.name(),
                    "PAYROLL-DISBURSEMENT:" + payment.getId() + ":V" + payment.getVersion(),
                    date,
                    "Payroll disbursement " + payment.getPeriodYear() + "/" + payment.getPeriodMonth()
                            + " employee " + payment.getEmployeeId(),
                    payment.getNetAmount(),
                    payment.getNetAmount(),
                    null,
                    payment.getEmployeeId(),
                    null,
                    actor
            );
            log.info("SalaryPayment disbursement posted paymentId={} journalId={}", payment.getId(), journal.getId());
            return journal;
        } catch (Exception e) {
            log.error("postDisbursement failed for paymentId={}", payment.getId(), e);
            throw e;
        }
    }

    @Transactional
    public JournalEntry reverseDisbursement(SalaryPayment payment, LocalDate reversalDate,
                                            String reason, String actor) {
        log.debug("reverseDisbursement called with paymentId={}, reversalDate={}, actor={}", payment.getId(), reversalDate, actor);
        if (payment.getPaymentJournalId() == null || payment.getPaymentJournalId().isBlank()) {
            log.warn("Validation failed: Payroll disbursement journal evidence is required before reversal paymentId={}", payment.getId());
            throw new BusinessRuleException(
                    "Payroll disbursement journal evidence is required before reversal.",
                    "SUBLEDGER_POSTING_NOT_FOUND",
                    HttpStatus.CONFLICT);
        }
        try {
            JournalEntry journal = subledgerPostingService.reverse(
                    payment.getPaymentJournalId(),
                    "PAYROLL-DISBURSEMENT-REVERSAL:" + payment.getId() + ":V" + payment.getVersion(),
                    reversalDate,
                    "Payroll payment reversal: " + reason,
                    actor
            );
            log.info("SalaryPayment disbursement reversed paymentId={} journalId={}", payment.getId(), journal.getId());
            return journal;
        } catch (Exception e) {
            log.error("reverseDisbursement failed for paymentId={}", payment.getId(), e);
            throw e;
        }
    }
}
