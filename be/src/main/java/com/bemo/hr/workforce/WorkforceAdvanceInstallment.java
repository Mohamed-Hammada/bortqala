package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workforce_advance_installments")
@Getter
public class WorkforceAdvanceInstallment {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "advance_id", nullable = false, length = 36) private String advanceId;
    @Column(name = "installment_number", nullable = false) private int installmentNumber;
    @Column(name = "due_date", nullable = false, length = 10) private String dueDate;
    @Column(precision = 12, scale = 2, nullable = false) private BigDecimal amount;
    @Column(name = "deducted_amount", precision = 12, scale = 2) private BigDecimal deductedAmount;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "period_id", length = 36) private String periodId;

    protected WorkforceAdvanceInstallment() { }

    public WorkforceAdvanceInstallment(String advanceId, int installmentNumber, String dueDate, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.advanceId = advanceId;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.deductedAmount = BigDecimal.ZERO;
        this.status = "PENDING";
    }

    public void applyDeduction(BigDecimal value, String periodId) {
        if (value == null) return;
        this.deductedAmount = this.deductedAmount.add(value);
        this.periodId = periodId;
        if (this.deductedAmount.compareTo(this.amount) >= 0) {
            this.status = "PAID";
        } else if (this.deductedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = "PARTIAL";
        }
    }
}
