package com.bemo.hr.finance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fx_postings")
public class FxPosting {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;
    @Column(name = "source_document_id", nullable = false, length = 36)
    private String sourceDocumentId;
    @Column(name = "foreign_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal foreignAmount;
    @Column(name = "transaction_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal transactionRate;
    @Column(name = "closing_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal closingRate;
    @Column(name = "gain_loss_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal gainLossAmount;
    @Column(name = "rate_source", nullable = false, length = 100)
    private String rateSource;
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    @Column(name = "fiscal_period_id", nullable = false, length = 36)
    private String fiscalPeriodId;
    @Column(name = "journal_entry_id", nullable = false, length = 36)
    private String journalEntryId;
    @Column(name = "reversal_journal_entry_id", length = 36)
    private String reversalJournalEntryId;
    @Column(name = "operation_id", nullable = false, length = 80)
    private String operationId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    protected FxPosting() {
    }
    public FxPosting(Type type, String sourceDocumentId, BigDecimal foreignAmount, BigDecimal transactionRate, BigDecimal closingRate, BigDecimal gainLossAmount, String rateSource, LocalDate effectiveDate, String fiscalPeriodId, String journalEntryId, String operationId) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.sourceDocumentId = sourceDocumentId;
        this.foreignAmount = foreignAmount;
        this.transactionRate = transactionRate;
        this.closingRate = closingRate;
        this.gainLossAmount = gainLossAmount;
        this.rateSource = rateSource;
        this.effectiveDate = effectiveDate;
        this.fiscalPeriodId = fiscalPeriodId;
        this.journalEntryId = journalEntryId;
        this.operationId = operationId;
        this.status = Status.POSTED;
    }

    public void reverse(String journalId) {
        if (status == Status.REVERSED) return;
        status = Status.REVERSED;
        reversalJournalEntryId = journalId;
    }

    @PrePersist
    void pre() {
        createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }

    public BigDecimal getGainLossAmount() {
        return gainLossAmount;
    }

    public String getRateSource() {
        return rateSource;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public String getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    public String getJournalEntryId() {
        return journalEntryId;
    }

    public String getReversalJournalEntryId() {
        return reversalJournalEntryId;
    }

    public String getOperationId() {
        return operationId;
    }

    public Status getStatus() {
        return status;
    }

    public enum Type {UNREALIZED, REALIZED}

    public enum Status {POSTED, REVERSED}
}
