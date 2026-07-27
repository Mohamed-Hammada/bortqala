package com.bemo.hr.payroll.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.TenantId;

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

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SalaryPayment() { }

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
        this.paymentStatus = paymentStatus == null ? PaymentStatus.PENDING : paymentStatus;
        this.paidAt = paidAt;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.note = note;
        this.createdBy = createdBy;
    }

    public void updateStatus(PaymentStatus nextStatus) {
        this.paymentStatus = nextStatus;
    }

    public void markAsReversed(String reason, String actor) {
        this.paymentStatus = PaymentStatus.REVERSED;
        this.note = (this.note == null ? "" : this.note + " | ") + "تم التراجع: " + reason;
        this.createdBy = actor;
    }

    public void markAsPaid(BigDecimal gross, BigDecimal advances, BigDecimal deductions,
                           BigDecimal bonus, BigDecimal net, PaymentMethod method,
                           Instant paidAtInstant, String refCode, String noteText, String actor) {
        this.grossAmount = gross == null ? this.grossAmount : gross;
        this.advancesDeducted = advances == null ? this.advancesDeducted : advances;
        this.otherDeductions = deductions == null ? this.otherDeductions : deductions;
        this.bonuses = bonus == null ? this.bonuses : bonus;
        this.netAmount = net == null ? this.grossAmount.subtract(this.advancesDeducted).subtract(this.otherDeductions).add(this.bonuses) : net;
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentMethod = method == null ? PaymentMethod.CASH : method;
        this.paidAt = paidAtInstant == null ? Instant.now() : paidAtInstant;
        this.referenceCode = refCode;
        this.note = noteText;
        this.createdBy = actor;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getEmployeeId() { return employeeId; }
    public String getReportId() { return reportId; }
    public int getPeriodYear() { return periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public String getPeriodKind() { return periodKind; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getAdvancesDeducted() { return advancesDeducted; }
    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public BigDecimal getBonuses() { return bonuses; }
    public BigDecimal getNetAmount() { return netAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public Instant getPaidAt() { return paidAt; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getReferenceCode() { return referenceCode; }
    public String getNote() { return note; }
    public String getCreatedBy() { return createdBy; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

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
