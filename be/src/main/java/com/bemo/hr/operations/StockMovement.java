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
@Table(name = "stock_movements")
@Getter
public class StockMovement {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "item_id", nullable = false) private String itemId;
    @Column(name = "party_id") private String partyId;
    @Column(name = "operation_type", nullable = false, length = 50) private String operationType;
    @Column(name = "quantity_delta", nullable = false, precision = 19, scale = 4) private BigDecimal quantityDelta;
    @Column(name = "loss_percentage", precision = 7, scale = 4) private BigDecimal lossPercentage;
    @Column(name = "reference_code", length = 100) private String referenceCode;
    @Column(length = 1000) private String note;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "created_by", nullable = false, length = 100) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected StockMovement() { }

    public StockMovement(String itemId, String partyId, String operationType, BigDecimal quantityDelta,
                         BigDecimal lossPercentage, String referenceCode, String note, Instant occurredAt, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.partyId = partyId;
        this.operationType = operationType.strip().toUpperCase();
        this.quantityDelta = quantityDelta;
        this.lossPercentage = lossPercentage;
        this.referenceCode = nullable(referenceCode);
        this.note = nullable(note);
        this.occurredAt = occurredAt;
        this.createdBy = createdBy;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
