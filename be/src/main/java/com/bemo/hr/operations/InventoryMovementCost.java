package com.bemo.hr.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movement_costs")
@Getter
public class InventoryMovementCost {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "movement_id", nullable = false, length = 36) private String movementId;
    @Column(name = "item_id", nullable = false, length = 36) private String itemId;
    @Enumerated(EnumType.STRING) @Column(name = "valuation_method", nullable = false, length = 30)
    private InventoryValuationPolicy.Method valuationMethod;
    @Column(name = "quantity_effect", nullable = false, precision = 19, scale = 4) private BigDecimal quantityEffect;
    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 6) private BigDecimal unitCost;
    @Column(name = "value_effect", nullable = false, precision = 19, scale = 2) private BigDecimal valueEffect;
    @Column(name = "journal_entry_id", length = 36) private String journalEntryId;
    @Column(nullable = false, length = 2000) private String explanation;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected InventoryMovementCost() { }

    public InventoryMovementCost(String movementId, String itemId, InventoryValuationPolicy.Method method,
                                 BigDecimal quantityEffect, BigDecimal unitCost, BigDecimal valueEffect,
                                 String explanation, Instant occurredAt) {
        this.id = UUID.randomUUID().toString();
        this.movementId = movementId;
        this.itemId = itemId;
        this.valuationMethod = method;
        this.quantityEffect = quantityEffect;
        this.unitCost = unitCost;
        this.valueEffect = valueEffect;
        this.explanation = explanation;
        this.occurredAt = occurredAt;
    }

    public void linkJournal(String journalEntryId) { this.journalEntryId = journalEntryId; }
    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
