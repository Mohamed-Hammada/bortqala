package com.bemo.hr.payroll.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalaryPaymentStateTransitionTests {

    private SalaryPayment paymentIn(PaymentStatus status) {
        SalaryPayment payment = new SalaryPayment("emp-1", "report-1", 2026, 8, "FULL_MONTH",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("5000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("5000"), PaymentStatus.DRAFT, null, null, null, null, "maker");
        while (payment.getPaymentStatus() != status && payment.getPaymentStatus() != PaymentStatus.POSTED) {
            switch (payment.getPaymentStatus()) {
                case DRAFT, PENDING -> payment.transitionTo(PaymentStatus.CALCULATED);
                case CALCULATED -> payment.transitionTo(PaymentStatus.REVIEWED);
                case REVIEWED -> payment.transitionTo(PaymentStatus.APPROVED);
                case APPROVED -> payment.transitionTo(PaymentStatus.POSTED);
                default -> {
                    return payment;
                }
            }
        }
        if (status == PaymentStatus.PAID && payment.getPaymentStatus() == PaymentStatus.POSTED) {
            payment.markAsPaid(PaymentMethod.CASH, null, null, null, "disburser");
        }
        return payment;
    }

    @Test
    void postedSalaryCanBePaid() {
        SalaryPayment payment = paymentIn(PaymentStatus.POSTED);

        payment.markAsPaid(PaymentMethod.BANK_TRANSFER, null, "REF-1", "note", "disburser");

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidBy()).isEqualTo("disburser");
        assertThat(payment.getReferenceCode()).isEqualTo("REF-1");
    }

    @Test
    void reversedSalaryCanNeverReturnToPaid() {
        SalaryPayment payment = paymentIn(PaymentStatus.PAID);
        payment.markAsReversed("wrong disbursement", "reverser");
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REVERSED);

        assertThatThrownBy(() -> payment.markAsPaid(PaymentMethod.CASH, null, "REF-2", null, "disburser"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo("PAYROLL_PAYMENT_STATE_INVALID"));
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REVERSED);
    }

    @Test
    void reversedIsTerminalForEveryLifecycleCommand() {
        SalaryPayment payment = paymentIn(PaymentStatus.PAID);
        payment.markAsReversed("duplicate", "reverser");

        for (PaymentStatus target : PaymentStatus.values()) {
            PaymentStatus source = payment.getPaymentStatus();
            if (target == PaymentStatus.PAID || target == PaymentStatus.CANCELLED) continue;
            assertThatThrownBy(() -> payment.transitionTo(target))
                    .as("REVERSED must not transition to %s", target)
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                            .isEqualTo("PAYROLL_STATE_TRANSITION_INVALID"));
            assertThat(payment.getPaymentStatus()).as("state must stay REVERSED").isEqualTo(source);
        }
        assertThatThrownBy(() -> payment.markAsReversed("again", "reverser"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo("PAYROLL_REVERSAL_STATE_INVALID"));
    }

    @Test
    void unpostedStatesCannotBePaidDirectly() {
        for (PaymentStatus status : new PaymentStatus[] {
                PaymentStatus.DRAFT, PaymentStatus.PENDING, PaymentStatus.CALCULATED,
                PaymentStatus.REVIEWED, PaymentStatus.APPROVED}) {
            SalaryPayment payment = new SalaryPayment("emp-1", "report-1", 2026, 8, "FULL_MONTH",
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                    new BigDecimal("5000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("5000"), status, null, null, null, null, "maker");
            assertThatThrownBy(() -> payment.markAsPaid(PaymentMethod.CASH, null, null, null, "actor"))
                    .as("%s salary must not be payable", status)
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                            .isEqualTo("PAYROLL_PAYMENT_STATE_INVALID"));
            assertThat(payment.getPaymentStatus()).isEqualTo(status);
        }
    }
}
