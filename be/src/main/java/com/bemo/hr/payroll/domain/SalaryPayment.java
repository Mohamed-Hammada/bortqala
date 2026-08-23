package com.bemo.hr.payroll.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salary_payment")
public class SalaryPayment {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String tenantId;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "report_id")
    private String reportId;

    @Column(name = "payroll_run_id", length = 36)
    private String payrollRunId;

    @Column(name = "payroll_snapshot_id", length = 36)
    private String payrollSnapshotId;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "period_kind", nullable = false)
    private String periodKind;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "advances_deducted", nullable = false)
    private BigDecimal advancesDeducted;

    @Column(name = "other_deductions", nullable = false)
    private BigDecimal otherDeductions;

    @Column(name = "bonuses", nullable = false)
    private BigDecimal bonuses;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "reference_code")
    private String referenceCode;

    @Column(name = "note")
    private String note;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "paid_by")
    private String paidBy;

    @Column(name = "reversed_by")
    private String reversedBy;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    @Column(name = "payment_journal_id", length = 36)
    private String paymentJournalId;

    @Column(name = "reversal_journal_id", length = 36)
    private String reversalJournalId;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SalaryPayment() {
    }

    public SalaryPayment(String employeeId, String reportId, int periodYear, int periodMonth,
                         String periodKind, LocalDate periodStart, LocalDate periodEnd,
                         BigDecimal grossAmount, BigDecimal advancesDeducted, BigDecimal otherDeductions,
                         BigDecimal bonuses, BigDecimal netAmount, PaymentStatus paymentStatus,
                         Instant paidAt, PaymentMethod paymentMethod, String referenceCode, String note,
                         String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.employeeId = employeeId;
        this.reportId = reportId;
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.periodKind = periodKind;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.grossAmount = grossAmount == null ? BigDecimal.ZERO : grossAmount;
        this.advancesDeducted = advancesDeducted == null ? BigDecimal.ZERO : advancesDeducted;
        this.otherDeductions = otherDeductions == null ? BigDecimal.ZERO : otherDeductions;
        this.bonuses = bonuses == null ? BigDecimal.ZERO : bonuses;
        this.netAmount = netAmount == null ? BigDecimal.ZERO : netAmount;
        this.paymentStatus = paymentStatus == null ? PaymentStatus.DRAFT : paymentStatus;
        this.paidAt = paidAt;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.note = note;
        this.createdBy = createdBy;
    }

    public void transitionTo(PaymentStatus nextStatus) {
        PaymentStatus expected = switch (this.paymentStatus) {
            case DRAFT, PENDING -> PaymentStatus.CALCULATED;
            case CALCULATED -> PaymentStatus.REVIEWED;
            case REVIEWED -> PaymentStatus.APPROVED;
            case APPROVED -> PaymentStatus.POSTED;
            default -> null;
        };
        if (nextStatus == null || nextStatus != expected) {
            throw new BusinessRuleException(
                    "Payroll status cannot transition from " + this.paymentStatus + " to " + nextStatus + ".",
                    "PAYROLL_STATE_TRANSITION_INVALID", HttpStatus.CONFLICT);
        }
        this.paymentStatus = nextStatus;
    }

    public void attachCalculationEvidence(String payrollRunId, String payrollSnapshotId) {
        if (this.payrollSnapshotId != null && !this.payrollSnapshotId.equals(payrollSnapshotId)) {
            throw new IllegalStateException("Payroll calculation evidence is immutable once attached");
        }
        this.payrollRunId = payrollRunId;
        this.payrollSnapshotId = payrollSnapshotId;
    }

    public void markAsReversed(String reason, String actor) {
        if (this.paymentStatus != PaymentStatus.PAID) {
            throw new BusinessRuleException("Only a paid salary can be reversed.",
                    "PAYROLL_REVERSAL_STATE_INVALID", HttpStatus.CONFLICT);
        }
        this.paymentStatus = PaymentStatus.REVERSED;
        this.reversalReason = reason;
        this.reversedBy = actor;
        this.reversedAt = Instant.now();
    }

    public void markAsPaid(PaymentMethod method, Instant paidAtInstant, String refCode,
                           String noteText, String actor) {
        if (this.paymentStatus != PaymentStatus.POSTED) {
            throw new BusinessRuleException("Only a posted salary can be paid.",
                    "PAYROLL_PAYMENT_STATE_INVALID", HttpStatus.CONFLICT);
        }
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentMethod = method == null ? PaymentMethod.CASH : method;
        this.paidAt = paidAtInstant == null ? Instant.now() : paidAtInstant;
        this.referenceCode = refCode;
        this.note = noteText;
        this.paidBy = actor;
    }

    public void attachPaymentJournal(String journalId) {
        if (journalId == null || journalId.isBlank()) throw new IllegalArgumentException("Payment journal is required");
        this.paymentJournalId = journalId;
    }

    public void attachReversalJournal(String journalId) {
        if (journalId == null || journalId.isBlank())
            throw new IllegalArgumentException("Reversal journal is required");
        this.reversalJournalId = journalId;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getReportId() {
        return reportId;
    }

    public String getPayrollRunId() {
        return payrollRunId;
    }

    public String getPayrollSnapshotId() {
        return payrollSnapshotId;
    }

    public int getPeriodYear() {
        return periodYear;
    }

    public int getPeriodMonth() {
        return periodMonth;
    }

    public String getPeriodKind() {
        return periodKind;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getAdvancesDeducted() {
        return advancesDeducted;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public BigDecimal getBonuses() {
        return bonuses;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getNote() {
        return note;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public String getReversedBy() {
        return reversedBy;
    }

    public Instant getReversedAt() {
        return reversedAt;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public String getPaymentJournalId() {
        return paymentJournalId;
    }

    public String getReversalJournalId() {
        return reversalJournalId;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
