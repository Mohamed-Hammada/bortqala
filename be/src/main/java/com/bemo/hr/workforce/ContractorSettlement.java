package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contractor_settlements")
@Getter
public class ContractorSettlement {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "period_id", nullable = false, length = 36) private String periodId;
    @Column(name = "contractor_id", nullable = false, length = 36) private String contractorId;
    @Column(name = "accounting_model", nullable = false, length = 50) private String accountingModel;
    @Column(name = "workers_net_total", precision = 12, scale = 2) private BigDecimal workersNetTotal;
    @Column(name = "contractor_rates_total", precision = 12, scale = 2) private BigDecimal contractorRatesTotal;
    @Column(name = "commission_amount", precision = 12, scale = 2) private BigDecimal commissionAmount;
    @Column(name = "fixed_amount", precision = 12, scale = 2) private BigDecimal fixedAmount;
    @Column(name = "additions_amount", precision = 12, scale = 2) private BigDecimal additionsAmount;
    @Column(name = "deductions_amount", precision = 12, scale = 2) private BigDecimal deductionsAmount;
    @Column(name = "gross_amount", precision = 12, scale = 2) private BigDecimal grossAmount;
    @Column(name = "net_payable", precision = 12, scale = 2) private BigDecimal netPayable;
    @Column(name = "paid_amount", precision = 12, scale = 2) private BigDecimal paidAmount;
    @Column(name = "invoice_number", length = 100) private String invoiceNumber;
    @Column(name = "invoice_date") private Instant invoiceDate;
    @Column(name = "posted_journal_entry_id", length = 36) private String postedJournalEntryId;
    @Column(name = "calculation_version") private Integer calculationVersion;
    @Column(nullable = false, length = 30) private String status;
    @Version private Long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ContractorSettlement() { }

    public ContractorSettlement(String periodId, String contractorId, String accountingModel,
                                BigDecimal workersNetTotal, BigDecimal contractorRatesTotal,
                                BigDecimal commissionAmount, BigDecimal fixedAmount,
                                BigDecimal additionsAmount, BigDecimal deductionsAmount,
                                BigDecimal grossAmount, BigDecimal netPayable, BigDecimal paidAmount, String status) {
        this.id = UUID.randomUUID().toString();
        this.periodId = periodId;
        this.contractorId = contractorId;
        this.accountingModel = accountingModel;
        this.workersNetTotal = workersNetTotal != null ? workersNetTotal : BigDecimal.ZERO;
        this.contractorRatesTotal = contractorRatesTotal != null ? contractorRatesTotal : BigDecimal.ZERO;
        this.commissionAmount = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
        this.fixedAmount = fixedAmount != null ? fixedAmount : BigDecimal.ZERO;
        this.additionsAmount = additionsAmount != null ? additionsAmount : BigDecimal.ZERO;
        this.deductionsAmount = deductionsAmount != null ? deductionsAmount : BigDecimal.ZERO;
        this.grossAmount = grossAmount != null ? grossAmount : BigDecimal.ZERO;
        this.netPayable = netPayable != null ? netPayable : BigDecimal.ZERO;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.calculationVersion = 1;
        this.status = status != null ? status.strip().toUpperCase() : "DRAFT";
    }

    public void linkInvoice(String invoiceNumber, Instant invoiceDate) {
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
    }

    public void markPosted(String journalEntryId) {
        this.postedJournalEntryId = journalEntryId;
        this.status = "POSTED";
    }

    public void updatePaidAmount(BigDecimal amount) {
        this.paidAmount = this.paidAmount.add(amount != null ? amount : BigDecimal.ZERO);
        if (this.paidAmount.compareTo(this.netPayable) >= 0) {
            this.status = "PAID";
        }
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
