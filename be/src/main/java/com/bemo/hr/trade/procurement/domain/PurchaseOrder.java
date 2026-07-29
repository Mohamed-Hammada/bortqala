package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    public enum Status {
        DRAFT,
        ISSUED,
        PARTIALLY_RECEIVED,
        RECEIVED,
        CANCELLED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "po_number", nullable = false, length = 50)
    private String poNumber;

    @Column(name = "po_date", nullable = false)
    private LocalDate poDate;

    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Column(name = "purchase_request_id", length = 36)
    private String purchaseRequestId;

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected PurchaseOrder() {}

    public PurchaseOrder(String poNumber, LocalDate poDate, String supplierId, String purchaseRequestId, String paymentTerms, BigDecimal totalAmount) {
        this(poNumber, poDate, supplierId, purchaseRequestId, paymentTerms, "EGP", totalAmount);
    }

    public PurchaseOrder(String poNumber, LocalDate poDate, String supplierId, String purchaseRequestId,
                         String paymentTerms, String currencyCode, BigDecimal totalAmount) {
        this.id = UUID.randomUUID().toString();
        this.poNumber = poNumber.strip();
        this.poDate = poDate;
        this.supplierId = supplierId;
        this.purchaseRequestId = purchaseRequestId == null || purchaseRequestId.isBlank() ? null : purchaseRequestId.strip();
        this.paymentTerms = paymentTerms == null ? null : paymentTerms.strip();
        this.currencyCode = currencyCode == null || currencyCode.isBlank() ? "EGP" : currencyCode.strip().toUpperCase();
        this.status = Status.DRAFT;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void updateDraft(String poNumber, LocalDate poDate, String supplierId, String purchaseRequestId,
                            String paymentTerms, String currencyCode, BigDecimal totalAmount) {
        if (status != Status.DRAFT) throw new IllegalStateException("Only draft purchase orders can be edited.");
        this.poNumber = poNumber.strip();
        this.poDate = poDate;
        this.supplierId = supplierId;
        this.purchaseRequestId = purchaseRequestId == null || purchaseRequestId.isBlank() ? null : purchaseRequestId.strip();
        this.paymentTerms = paymentTerms == null || paymentTerms.isBlank() ? null : paymentTerms.strip();
        this.currencyCode = currencyCode == null || currencyCode.isBlank() ? "EGP" : currencyCode.strip().toUpperCase();
        this.totalAmount = totalAmount;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getPoNumber() { return poNumber; }
    public LocalDate getPoDate() { return poDate; }
    public String getSupplierId() { return supplierId; }
    public String getPurchaseRequestId() { return purchaseRequestId; }
    public String getPaymentTerms() { return paymentTerms; }
    public String getCurrencyCode() { return currencyCode; }
    public Status getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
