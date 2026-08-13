package com.bemo.hr.workforce.domain;

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
@Table(name = "workforce_invoice_matches")
public class WorkforceInvoiceMatch {

    public enum Status {
        MATCHED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "settlement_id", nullable = false, length = 36)
    private String settlementId;

    @Column(name = "invoice_id", nullable = false, length = 36)
    private String invoiceId;

    @Column(name = "matched_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal matchedAmount;

    @Column(name = "variance_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal varianceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.MATCHED;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WorkforceInvoiceMatch() {}

    public WorkforceInvoiceMatch(String settlementId, String invoiceId, BigDecimal matchedAmount, BigDecimal varianceAmount) {
        this.id = UUID.randomUUID().toString();
        this.settlementId = settlementId;
        this.invoiceId = invoiceId;
        this.matchedAmount = matchedAmount;
        this.varianceAmount = varianceAmount;
        this.status = Status.MATCHED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getSettlementId() { return settlementId; }
    public String getInvoiceId() { return invoiceId; }
    public BigDecimal getMatchedAmount() { return matchedAmount; }
    public BigDecimal getVarianceAmount() { return varianceAmount; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
