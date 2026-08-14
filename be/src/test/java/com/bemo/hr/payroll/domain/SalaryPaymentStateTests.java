package com.bemo.hr.payroll.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalaryPaymentStateTests {

    @Test
    void enforcesTheCompletePayrollStateGraphWithoutSkipping() {
        SalaryPayment payment = draft();

        assertThatThrownBy(() -> payment.transitionTo(PaymentStatus.PAID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PAYROLL_STATE_TRANSITION_INVALID");

        payment.transitionTo(PaymentStatus.CALCULATED);
        payment.transitionTo(PaymentStatus.REVIEWED);
        payment.transitionTo(PaymentStatus.APPROVED);
        payment.transitionTo(PaymentStatus.POSTED);
        payment.markAsPaid(PaymentMethod.BANK_TRANSFER, Instant.parse("2026-08-13T10:00:00Z"),
                "BANK-1", "paid", "cashier");

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getCreatedBy()).isEqualTo("maker");
        assertThat(payment.getPaidBy()).isEqualTo("cashier");
    }

    @Test
    void unpaidSalaryCannotBeReversed() {
        assertThatThrownBy(() -> draft().markAsReversed("invalid", "auditor"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PAYROLL_REVERSAL_STATE_INVALID");
    }

    @Test
    void paidSalaryCanBeReversedOnceWithoutOverwritingCreatorOrPayer() {
        SalaryPayment payment = draft();
        payment.transitionTo(PaymentStatus.CALCULATED);
        payment.transitionTo(PaymentStatus.REVIEWED);
        payment.transitionTo(PaymentStatus.APPROVED);
        payment.transitionTo(PaymentStatus.POSTED);
        payment.markAsPaid(PaymentMethod.CASH, Instant.now(), null, null, "cashier");
        payment.markAsReversed("duplicate bank transfer", "controller");

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REVERSED);
        assertThat(payment.getCreatedBy()).isEqualTo("maker");
        assertThat(payment.getPaidBy()).isEqualTo("cashier");
        assertThat(payment.getReversedBy()).isEqualTo("controller");
        assertThat(payment.getReversedAt()).isNotNull();
        assertThat(payment.getReversalReason()).isEqualTo("duplicate bank transfer");
        assertThatThrownBy(() -> payment.markAsReversed("again", "controller"))
                .isInstanceOf(BusinessRuleException.class);
    }

    private SalaryPayment draft() {
        return new SalaryPayment("employee-1", "report-1", 2026, 8, "FULL_MONTH",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("5000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("5000.00"), PaymentStatus.DRAFT, null, null, null, null, "maker");
    }
}
