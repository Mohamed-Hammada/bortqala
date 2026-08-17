package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "supplier_quote_headers")
public class SupplierQuoteHeader {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "rfq_id", nullable = false, length = 36)
    private String rfqId;
    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;
    @Column(name = "quote_number", nullable = false, length = 50)
    private String quoteNumber;
    @Column(name = "quote_date", nullable = false)
    private LocalDate quoteDate;
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.SUBMITTED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected SupplierQuoteHeader() {
    }

    public SupplierQuoteHeader(String rfqId, String supplierId, String quoteNumber, LocalDate quoteDate, LocalDate validUntil, BigDecimal totalAmount) {
        this.id = UUID.randomUUID().toString();
        this.rfqId = rfqId;
        this.supplierId = supplierId;
        this.quoteNumber = quoteNumber;
        this.quoteDate = quoteDate;
        this.validUntil = validUntil;
        this.totalAmount = totalAmount;
        this.status = Status.SUBMITTED;
    }

    public void markEvaluated() {
        this.status = Status.EVALUATED;
    }

    public void award() {
        this.status = Status.AWARDED;
    }

    public void reject() {
        this.status = Status.REJECTED;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
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

    public String getSupplierId() {
        return supplierId;
    }

    public String getQuoteNumber() {
        return quoteNumber;
    }

    public LocalDate getQuoteDate() {
        return quoteDate;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Status getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public enum Status {
        SUBMITTED, EVALUATED, AWARDED, REJECTED
    }
}
