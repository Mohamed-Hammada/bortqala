package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vendor_payment_proposals")
public class VendorPaymentProposal {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "proposal_number", nullable = false, length = 50)
    private String proposalNumber;
    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;
    @Column(name = "invoice_id", nullable = false, length = 36)
    private String invoiceId;
    @Column(name = "proposed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal proposedAmount;
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "executed_by", length = 100)
    private String executedBy;
    @Column(name = "operation_id", length = 80)
    private String operationId;
    @Column(name = "supplier_payment_id", length = 36)
    private String supplierPaymentId;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PROPOSED;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected VendorPaymentProposal() {
    }

    public VendorPaymentProposal(String supplierId, String invoiceId, BigDecimal proposedAmount,
                                 String currencyCode, LocalDate dueDate, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.proposalNumber = "PROP-" + System.currentTimeMillis();
        this.supplierId = supplierId;
        this.invoiceId = invoiceId;
        this.proposedAmount = proposedAmount;
        this.currencyCode = currencyCode;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
        this.status = Status.PROPOSED;
    }

    public void approve(String actor) {
        if (status != Status.PROPOSED) throw new IllegalStateException("Only proposed payments can be approved");
        this.status = Status.APPROVED;
        this.approvedBy = actor;
    }

    public void execute(String operationId, String supplierPaymentId, String actor) {
        if (status != Status.APPROVED) throw new IllegalStateException("Only approved payments can be executed");
        this.status = Status.EXECUTED;
        this.operationId = operationId;
        this.supplierPaymentId = supplierPaymentId;
        this.executedBy = actor;
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

    public String getProposalNumber() {
        return proposalNumber;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getProposedAmount() {
        return proposedAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getSupplierPaymentId() {
        return supplierPaymentId;
    }

    public LocalDate getDueDate() {
        return dueDate;
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
        PROPOSED, APPROVED, REJECTED, EXECUTED
    }
}
