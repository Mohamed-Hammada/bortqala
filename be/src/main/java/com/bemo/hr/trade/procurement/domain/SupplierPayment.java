package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "supplier_payments")
public class SupplierPayment {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "payment_number", nullable = false, length = 50)
    private String paymentNumber;
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;
    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;
    @Column(name = "supplier_invoice_id", nullable = false, length = 36)
    private String supplierInvoiceId;
    @Column(name = "operation_id", nullable = false, length = 80)
    private String operationId;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(name = "settlement_discount", nullable = false, precision = 15, scale = 2)
    private BigDecimal settlementDiscount = BigDecimal.ZERO;
    @Column(name = "original_due", precision = 15, scale = 2)
    private BigDecimal originalDue;

    public BigDecimal getSettlementDiscount() { return settlementDiscount == null ? BigDecimal.ZERO : settlementDiscount; }

    public void applySettlementDiscount(BigDecimal discount) { this.settlementDiscount = discount; }

    public BigDecimal getOriginalDue() { return originalDue; }

    public void applyOriginalDue(BigDecimal due) { this.originalDue = due; }
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;
    @Column(name = "beneficiary_bank_account", length = 100)
    private String beneficiaryBankAccount;
    @Column(length = 500)
    private String notes;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SupplierPayment() {
    }

    public SupplierPayment(String paymentNumber, LocalDate paymentDate, String supplierId,
                           String supplierInvoiceId, String operationId, BigDecimal amount, String paymentMethod, String notes) {
        this.id = UUID.randomUUID().toString();
        this.paymentNumber = paymentNumber.strip();
        this.paymentDate = paymentDate;
        this.supplierId = supplierId;
        this.supplierInvoiceId = supplierInvoiceId;
        this.operationId = operationId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
        this.status = "POSTED";
    }

    public void freezeBeneficiaryBankAccount(String account) {
        if (beneficiaryBankAccount == null)
            beneficiaryBankAccount = account == null || account.isBlank() ? null : account.strip();
    }

    public String getId() {
        return id;
    }

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getSupplierInvoiceId() {
        return supplierInvoiceId;
    }

    public String getOperationId() {
        return operationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getBeneficiaryBankAccount() {
        return beneficiaryBankAccount;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }
}
