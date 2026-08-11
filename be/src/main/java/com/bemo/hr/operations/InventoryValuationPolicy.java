package com.bemo.hr.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_valuation_policies")
@Getter
public class InventoryValuationPolicy {
    public enum Method { FIFO, WEIGHTED_AVERAGE }

    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Enumerated(EnumType.STRING) @Column(name = "valuation_method", nullable = false, length = 30) private Method valuationMethod;
    @Column(name = "inventory_account_id", length = 36) private String inventoryAccountId;
    @Column(name = "receipt_offset_account_id", length = 36) private String receiptOffsetAccountId;
    @Column(name = "cogs_account_id", length = 36) private String cogsAccountId;
    @Column(name = "adjustment_account_id", length = 36) private String adjustmentAccountId;
    @Column(name = "gl_posting_enabled", nullable = false) private boolean glPostingEnabled;
    @Column(name = "allow_backdated_posting", nullable = false) private boolean allowBackdatedPosting;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public InventoryValuationPolicy() {
        this.id = UUID.randomUUID().toString();
        this.valuationMethod = Method.WEIGHTED_AVERAGE;
    }

    public void update(Method method, String inventoryAccountId, String receiptOffsetAccountId,
                       String cogsAccountId, String adjustmentAccountId, boolean glPostingEnabled,
                       boolean allowBackdatedPosting) {
        this.valuationMethod = method;
        this.inventoryAccountId = clean(inventoryAccountId);
        this.receiptOffsetAccountId = clean(receiptOffsetAccountId);
        this.cogsAccountId = clean(cogsAccountId);
        this.adjustmentAccountId = clean(adjustmentAccountId);
        this.glPostingEnabled = glPostingEnabled;
        this.allowBackdatedPosting = allowBackdatedPosting;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
