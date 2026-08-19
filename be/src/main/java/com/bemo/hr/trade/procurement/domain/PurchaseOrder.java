package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

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
    @Column(name = "department_id", length = 36)
    private String departmentId;
    @Column(name = "project_id", length = 36)
    private String projectId;
    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;
    @Column(name = "cost_code_id", length = 36)
    private String costCodeId;
    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;
    @Column(name = "base_currency_code", nullable = false, length = 10)
    private String baseCurrencyCode = "EGP";
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;
    @Column(name = "exchange_rate_date", nullable = false)
    private LocalDate exchangeRateDate;
    @Column(name = "exchange_rate_source", nullable = false, length = 50)
    private String exchangeRateSource = "BASE_CURRENCY";
    @Column(name = "exchange_rate_override_reason", length = 500)
    private String exchangeRateOverrideReason;
    @Column(name = "base_total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseTotalAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PurchaseOrder() {
    }

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
        this.exchangeRateDate = poDate;
        this.baseTotalAmount = this.totalAmount;
    }

    public void assignDepartment(String departmentId) {
        this.departmentId = departmentId == null || departmentId.isBlank() ? null : departmentId.strip();
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

    public void applyExchangeRate(String baseCurrencyCode, BigDecimal exchangeRate, LocalDate rateDate,
                                  String source, String overrideReason) {
        if (exchangeRate == null || exchangeRate.signum() <= 0) {
            throw new IllegalArgumentException("Exchange rate must be greater than zero.");
        }
        this.baseCurrencyCode = baseCurrencyCode;
        this.exchangeRate = exchangeRate;
        this.exchangeRateDate = rateDate;
        this.exchangeRateSource = source;
        this.exchangeRateOverrideReason = overrideReason == null || overrideReason.isBlank() ? null : overrideReason.strip();
        this.baseTotalAmount = totalAmount.multiply(exchangeRate).setScale(2, java.math.RoundingMode.HALF_UP);
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

    public String getPoNumber() {
        return poNumber;
    }

    public LocalDate getPoDate() {
        return poDate;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getPurchaseRequestId() {
        return purchaseRequestId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getBaseCurrencyCode() {
        return baseCurrencyCode;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public LocalDate getExchangeRateDate() {
        return exchangeRateDate;
    }

    public String getExchangeRateSource() {
        return exchangeRateSource;
    }

    public String getExchangeRateOverrideReason() {
        return exchangeRateOverrideReason;
    }

    public BigDecimal getBaseTotalAmount() {
        return baseTotalAmount;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void assignProject(String projectId, String wbsNodeId, String costCodeId) {
        this.projectId = projectId == null || projectId.isBlank() ? null : projectId.strip();
        this.wbsNodeId = wbsNodeId == null || wbsNodeId.isBlank() ? null : wbsNodeId.strip();
        this.costCodeId = costCodeId == null || costCodeId.isBlank() ? null : costCodeId.strip();
    }

    public String getProjectId() {
        return projectId;
    }

    public String getWbsNodeId() {
        return wbsNodeId;
    }

    public String getCostCodeId() {
        return costCodeId;
    }

    public long getVersion() {
        return version;
    }

    public enum Status {
        DRAFT,
        ISSUED,
        PARTIALLY_RECEIVED,
        RECEIVED,
        CANCELLED
    }
}
