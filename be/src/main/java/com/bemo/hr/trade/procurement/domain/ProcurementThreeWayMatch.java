package com.bemo.hr.trade.procurement.domain;

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
@Table(name = "procurement_three_way_matches")
@Getter
public class ProcurementThreeWayMatch {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "purchase_order_id", nullable = false, length = 36) private String purchaseOrderId;
    @Column(name = "goods_receipt_id", length = 36) private String goodsReceiptId;
    @Column(name = "supplier_invoice_id", nullable = false, length = 36) private String supplierInvoiceId;
    @Column(name = "match_status", nullable = false, length = 30) private String matchStatus;
    @Column(name = "price_variance_amount", precision = 19, scale = 2) private BigDecimal priceVarianceAmount;
    @Column(name = "quantity_variance_amount", precision = 19, scale = 2) private BigDecimal quantityVarianceAmount;
    @Column(name = "tolerance_percentage", precision = 5, scale = 2) private BigDecimal tolerancePercentage;
    @Column(name = "variance_reason", length = 500) private String varianceReason;
    @Column(name = "resolved_by", length = 160) private String resolvedBy;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ProcurementThreeWayMatch() { }

    public ProcurementThreeWayMatch(String purchaseOrderId, String goodsReceiptId, String supplierInvoiceId,
                                   String matchStatus, BigDecimal priceVarianceAmount, BigDecimal quantityVarianceAmount,
                                   BigDecimal tolerancePercentage, String varianceReason) {
        this.id = UUID.randomUUID().toString();
        this.purchaseOrderId = purchaseOrderId;
        this.goodsReceiptId = goodsReceiptId;
        this.supplierInvoiceId = supplierInvoiceId;
        this.matchStatus = matchStatus.strip().toUpperCase();
        this.priceVarianceAmount = priceVarianceAmount != null ? priceVarianceAmount : BigDecimal.ZERO;
        this.quantityVarianceAmount = quantityVarianceAmount != null ? quantityVarianceAmount : BigDecimal.ZERO;
        this.tolerancePercentage = tolerancePercentage != null ? tolerancePercentage : BigDecimal.ZERO;
        this.varianceReason = varianceReason;
        this.createdAt = Instant.now();
    }

    public void resolve(String resolvedBy, String resolutionNotes) {
        this.matchStatus = "RESOLVED";
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Instant.now();
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            this.varianceReason = (this.varianceReason != null ? this.varianceReason + " | " : "") + "Resolution: " + resolutionNotes.strip();
        }
    }

    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
