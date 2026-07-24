package com.bemo.hr.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_ledger_entries")
@Getter
public class PartnerLedgerEntry {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "party_id", nullable = false) private String partyId;
    @Column(name = "entry_type", nullable = false, length = 50) private String entryType;
    @Column(name = "amount_delta", nullable = false, precision = 19, scale = 2) private BigDecimal amountDelta;
    @Column(name = "reference_code", length = 100) private String referenceCode;
    @Column(length = 1000) private String note;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "created_by", nullable = false, length = 100) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PartnerLedgerEntry() { }

    public PartnerLedgerEntry(String partyId, String entryType, BigDecimal amountDelta, String referenceCode,
                              String note, Instant occurredAt, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.partyId = partyId;
        this.entryType = entryType.strip().toUpperCase();
        this.amountDelta = amountDelta;
        this.referenceCode = nullable(referenceCode);
        this.note = nullable(note);
        this.occurredAt = occurredAt;
        this.createdBy = createdBy;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
