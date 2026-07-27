package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    public enum Status {
        DRAFT,
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

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "posted_at")
    private Long postedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

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
        this.status = Status.POSTED;
        this.postedBy = username;
        this.postedAt = System.currentTimeMillis();
    }

    public void reverse() {
        this.status = Status.REVERSED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getEntryNumber() { return entryNumber; }
    public LocalDate getEntryDate() { return entryDate; }
    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public Status getStatus() { return status; }
    public String getFiscalPeriodId() { return fiscalPeriodId; }
    public String getPostedBy() { return postedBy; }
    public Long getPostedAt() { return postedAt; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
