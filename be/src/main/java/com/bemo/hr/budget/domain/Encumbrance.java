package com.bemo.hr.budget.domain;

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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "encumbrances")
public class Encumbrance {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "budget_id", nullable = false, length = 36)
    private String budgetId;

    @Column(name = "purchase_order_id", nullable = false, length = 36)
    private String purchaseOrderId;

    @Column(name = "purchase_order_number", nullable = false, length = 50)
    private String purchaseOrderNumber;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType = "PURCHASE_ORDER";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EncumbranceStatus status = EncumbranceStatus.ACTIVE;

    @Column(name = "committed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal committedAmount;

    @Column(name = "liquidated_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal liquidatedAmount = BigDecimal.ZERO;

    @Column(name = "released_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "committed_at", nullable = false)
    private long committedAt;

    @Column(name = "released_at")
    private Long releasedAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Encumbrance() {}

    public Encumbrance(String budgetId, String purchaseOrderId, String purchaseOrderNumber,
                       BigDecimal committedAmount, String currencyCode) {
        this.id = UUID.randomUUID().toString();
        this.budgetId = budgetId;
        this.purchaseOrderId = purchaseOrderId;
        this.purchaseOrderNumber = purchaseOrderNumber;
        this.committedAmount = committedAmount == null ? BigDecimal.ZERO : committedAmount;
        this.currencyCode = currencyCode == null || currencyCode.isBlank() ? "EGP" : currencyCode.strip().toUpperCase();
    }

    public void liquidate(BigDecimal amount) {
        BigDecimal remaining = committedAmount.subtract(liquidatedAmount);
        BigDecimal applied = amount == null || amount.signum() <= 0 ? BigDecimal.ZERO : amount.min(remaining).max(BigDecimal.ZERO);
        liquidatedAmount = liquidatedAmount.add(applied);
        if (status == EncumbranceStatus.ACTIVE && liquidatedAmount.compareTo(committedAmount) >= 0) {
            status = EncumbranceStatus.RELEASED;
            releasedAt = System.currentTimeMillis();
        }
    }

    public void release(BigDecimal released) {
        if (status == EncumbranceStatus.RELEASED) return;
        BigDecimal remaining = committedAmount.subtract(liquidatedAmount);
        BigDecimal toRelease = (released == null || released.signum() <= 0)
                ? remaining
                : released.min(remaining).max(BigDecimal.ZERO);
        releasedAmount = releasedAmount.add(toRelease);
        status = EncumbranceStatus.RELEASED;
        releasedAt = System.currentTimeMillis();
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
        committedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getBudgetId() { return budgetId; }
    public String getPurchaseOrderId() { return purchaseOrderId; }
    public String getPurchaseOrderNumber() { return purchaseOrderNumber; }
    public String getDocumentType() { return documentType; }
    public EncumbranceStatus getStatus() { return status; }
    public BigDecimal getCommittedAmount() { return committedAmount; }
    public BigDecimal getLiquidatedAmount() { return liquidatedAmount; }
    public BigDecimal getReleasedAmount() { return releasedAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public long getCommittedAt() { return committedAt; }
    public Long getReleasedAt() { return releasedAt; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
