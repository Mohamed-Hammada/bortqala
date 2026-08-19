package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sales_quotations")
public class SalesQuotation {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "quotation_number", nullable = false, length = 50)
    private String quotationNumber;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "quotation_date", nullable = false)
    private LocalDate quoteDate;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private QuotationStatus status;

    @Column(name = "terms_and_conditions", length = 2000)
    private String termsAndConditions;

    @Column(name = "sales_order_id", length = 36)
    private String salesOrderId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected SalesQuotation() {
    }

    public SalesQuotation(String quotationNumber, String customerId, LocalDate quoteDate, LocalDate validUntil, String termsAndConditions) {
        this.id = UUID.randomUUID().toString();
        this.quotationNumber = quotationNumber;
        this.customerId = customerId;
        this.quoteDate = quoteDate;
        this.validUntil = validUntil;
        this.termsAndConditions = termsAndConditions;
        this.subtotal = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.status = QuotationStatus.DRAFT;
    }

    public void updateTotals(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal totalAmount) {
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }

    public void send() {
        this.status = QuotationStatus.SENT;
    }

    public void accept() {
        this.status = QuotationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = QuotationStatus.REJECTED;
    }

    public void markConverted(String salesOrderId) {
        this.status = QuotationStatus.CONVERTED;
        this.salesOrderId = salesOrderId;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public LocalDate getQuoteDate() {
        return quoteDate;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public String getSalesOrderId() {
        return salesOrderId;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
