package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workforce_advance_ledger_entries")
@Getter
public class WorkforceAdvanceLedgerEntry {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "advance_id", nullable = false, length = 36)
    private String advanceId;
    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;
    @Column(name = "balance_after", precision = 12, scale = 2, nullable = false)
    private BigDecimal balanceAfter;
    @Column(length = 500)
    private String notes;
    @Column(name = "created_by", length = 160)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkforceAdvanceLedgerEntry() {
    }

    public WorkforceAdvanceLedgerEntry(String advanceId, String entryType, BigDecimal amount, BigDecimal balanceAfter, String notes, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.advanceId = advanceId;
        this.entryType = entryType != null ? entryType.strip().toUpperCase() : "ISSUANCE";
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.balanceAfter = balanceAfter != null ? balanceAfter : BigDecimal.ZERO;
        this.notes = notes;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
