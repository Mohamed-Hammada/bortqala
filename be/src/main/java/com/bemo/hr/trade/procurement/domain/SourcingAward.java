package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sourcing_awards")
public class SourcingAward {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "rfq_id", nullable = false, length = 36)
    private String rfqId;

    @Column(name = "quote_id", nullable = false, length = 36)
    private String quoteId;

    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Column(name = "awarded_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal awardedAmount;

    @Column(name = "purchase_order_id", length = 36)
    private String purchaseOrderId;

    @Column(name = "awarded_by", nullable = false, length = 100)
    private String awardedBy;

    @Column(name = "awarded_at", nullable = false)
    private long awardedAt;

    protected SourcingAward() {
    }

    public SourcingAward(String rfqId, String quoteId, String supplierId, BigDecimal awardedAmount, String purchaseOrderId, String awardedBy) {
        this.id = UUID.randomUUID().toString();
        this.rfqId = rfqId;
        this.quoteId = quoteId;
        this.supplierId = supplierId;
        this.awardedAmount = awardedAmount;
        this.purchaseOrderId = purchaseOrderId;
        this.awardedBy = awardedBy;
    }

    @PrePersist
    void prePersist() {
        awardedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getRfqId() {
        return rfqId;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public BigDecimal getAwardedAmount() {
        return awardedAmount;
    }

    public String getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public String getAwardedBy() {
        return awardedBy;
    }

    public long getAwardedAt() {
        return awardedAt;
    }
}
