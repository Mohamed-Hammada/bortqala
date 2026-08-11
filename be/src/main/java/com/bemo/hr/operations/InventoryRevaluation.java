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
@Table(name = "inventory_revaluations")
@Getter
public class InventoryRevaluation {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "item_id", nullable = false, length = 36) private String itemId;
    @Column(name = "operation_id", nullable = false, length = 80) private String operationId;
    @Column(name = "quantity_on_hand", nullable = false, precision = 19, scale = 4) private BigDecimal quantityOnHand;
    @Column(name = "old_value", nullable = false, precision = 19, scale = 2) private BigDecimal oldValue;
    @Column(name = "new_value", nullable = false, precision = 19, scale = 2) private BigDecimal newValue;
    @Column(name = "value_difference", nullable = false, precision = 19, scale = 2) private BigDecimal valueDifference;
    @Column(nullable = false, length = 1000) private String reason;
    @Column(name = "journal_entry_id", length = 36) private String journalEntryId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "created_by", nullable = false, length = 100) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected InventoryRevaluation() { }

    public InventoryRevaluation(String itemId, String operationId, BigDecimal quantityOnHand, BigDecimal oldValue,
                                BigDecimal newValue, String reason, Instant occurredAt, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.operationId = operationId;
        this.quantityOnHand = quantityOnHand;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.valueDifference = newValue.subtract(oldValue);
        this.reason = reason.strip();
        this.occurredAt = occurredAt;
        this.createdBy = createdBy;
    }

    public void linkJournal(String journalEntryId) { this.journalEntryId = journalEntryId; }
    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
