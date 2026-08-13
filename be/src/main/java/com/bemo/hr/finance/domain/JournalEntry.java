package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    public enum Status {
        DRAFT,
        APPROVED,
        REJECTED,
        POSTED,
        REVERSED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "entry_number", nullable = false, length = 50)
    private String entryNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "fiscal_period_id", length = 36)
    private String fiscalPeriodId;

    @Column(length = 10)
    private String currency;

    @Column(name = "reversal_entry_id", length = 36)
    private String reversalEntryId;

    @Column(name = "reversed_entry_id", length = 36)
    private String reversedEntryId;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    @Column(name = "reversed_by", length = 100)
    private String reversedBy;

    @Column(name = "reversed_at")
    private Long reversedAt;

    @Column(name = "operation_id", length = 80)
    private String operationId;

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "posted_at")
    private Long postedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected JournalEntry() {}

    public JournalEntry(String entryNumber, LocalDate entryDate, String description, String reference, String fiscalPeriodId) {
        this.id = UUID.randomUUID().toString();
        this.entryNumber = entryNumber.strip();
        this.entryDate = entryDate;
        this.description = description.strip();
        this.reference = reference == null ? null : reference.strip();
        this.status = Status.DRAFT;
        this.fiscalPeriodId = fiscalPeriodId;
    }

    public void post(String username) {
        if (this.status != Status.APPROVED) {
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "لا يمكن ترحيل قيد في حالة " + this.status + ". الترحيل مسموح فقط من حالة مسودة.",
                    "JOURNAL_STATE_INVALID", org.springframework.http.HttpStatus.CONFLICT);
        }
        this.status = Status.POSTED;
        this.postedBy = username;
        this.postedAt = System.currentTimeMillis();
    }

    public void approve(String username) {
        if (status != Status.DRAFT) throw new com.bemo.hr.shared.domain.BusinessRuleException(
                "Only draft journals can be approved.", "JOURNAL_STATE_INVALID", org.springframework.http.HttpStatus.CONFLICT);
        status = Status.APPROVED; approvedBy = username; rejectionReason = null;
    }

    public void reject(String username, String reason) {
        if (status != Status.DRAFT) throw new com.bemo.hr.shared.domain.BusinessRuleException(
                "Only draft journals can be rejected.", "JOURNAL_STATE_INVALID", org.springframework.http.HttpStatus.CONFLICT);
        status = Status.REJECTED; approvedBy = username; rejectionReason = reason == null ? null : reason.strip();
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public void assignCreator(String username) { this.createdBy = username; }

    public void attachFiscalPeriod(String fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void markReversed(String reversalEntryId, String reversalReason, String reversedBy, String operationId) {
        if (this.status != Status.POSTED) {
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "لا يمكن عكس قيد في حالة " + this.status + ". العكس مسموح فقط للقيد المُرحَّل.",
                    "JOURNAL_STATE_INVALID", org.springframework.http.HttpStatus.CONFLICT);
        }
        this.status = Status.REVERSED;
        this.reversalEntryId = reversalEntryId;
        this.reversalReason = reversalReason;
        this.reversedBy = reversedBy;
        this.reversedAt = System.currentTimeMillis();
        this.operationId = operationId;
    }

    public void linkReversalOf(String reversedEntryId, String operationId) {
        this.reversedEntryId = reversedEntryId;
        this.operationId = operationId;
        this.status = Status.POSTED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getEntryNumber() { return entryNumber; }
    public LocalDate getEntryDate() { return entryDate; }
    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public Status getStatus() { return status; }
    public String getFiscalPeriodId() { return fiscalPeriodId; }
    public String getCurrency() { return currency; }
    public String getReversalEntryId() { return reversalEntryId; }
    public String getReversedEntryId() { return reversedEntryId; }
    public String getReversalReason() { return reversalReason; }
    public String getReversedBy() { return reversedBy; }
    public Long getReversedAt() { return reversedAt; }
    public String getOperationId() { return operationId; }
    public String getPostedBy() { return postedBy; }
    public String getCreatedBy() { return createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public Long getPostedAt() { return postedAt; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
