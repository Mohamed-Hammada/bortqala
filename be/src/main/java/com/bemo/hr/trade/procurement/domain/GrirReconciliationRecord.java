package com.bemo.hr.trade.procurement.domain;

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
@Table(name = "grir_reconciliation_records")
public class GrirReconciliationRecord {

    public enum Status {
        BALANCED, VARIANCE, CLOSED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "goods_receipt_line_id", nullable = false, length = 36)
    private String goodsReceiptLineId;

    @Column(name = "invoice_line_id", nullable = false, length = 36)
    private String invoiceLineId;

    @Column(name = "received_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal receivedAmount;

    @Column(name = "invoiced_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal invoicedAmount;

    @Column(name = "variance_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal varianceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.BALANCED;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected GrirReconciliationRecord() {}

    public GrirReconciliationRecord(String goodsReceiptLineId, String invoiceLineId, BigDecimal receivedAmount, BigDecimal invoicedAmount) {
        this.id = UUID.randomUUID().toString();
        this.goodsReceiptLineId = goodsReceiptLineId;
        this.invoiceLineId = invoiceLineId;
        this.receivedAmount = receivedAmount;
        this.invoicedAmount = invoicedAmount;
        this.varianceAmount = receivedAmount.subtract(invoicedAmount);
        this.status = this.varianceAmount.compareTo(BigDecimal.ZERO) == 0 ? Status.BALANCED : Status.VARIANCE;
    }

    public void close() {
        this.status = Status.CLOSED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getGoodsReceiptLineId() { return goodsReceiptLineId; }
    public String getInvoiceLineId() { return invoiceLineId; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public BigDecimal getInvoicedAmount() { return invoicedAmount; }
    public BigDecimal getVarianceAmount() { return varianceAmount; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
