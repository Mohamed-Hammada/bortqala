package com.bemo.hr.trade.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vendor_payment_proposal_allocations")
public class VendorPaymentProposalAllocation {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "proposal_id", nullable = false, length = 36)
    private String proposalId;

    @Column(name = "invoice_id", nullable = false, length = 36)
    private String invoiceId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "supplier_payment_id", length = 36)
    private String supplierPaymentId;

    @Column(name = "payment_operation_id", length = 80)
    private String paymentOperationId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected VendorPaymentProposalAllocation() { }

    public VendorPaymentProposalAllocation(String proposalId, int lineNo, String invoiceId, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.proposalId = proposalId;
        this.lineNo = lineNo;
        this.invoiceId = invoiceId;
        this.amount = amount;
    }

    public void linkPayment(String supplierPaymentId, String paymentOperationId) {
        if (this.supplierPaymentId != null && !this.supplierPaymentId.equals(supplierPaymentId)) {
            throw new IllegalStateException("Payment proposal allocation is already linked to a different payment");
        }
        this.supplierPaymentId = supplierPaymentId;
        this.paymentOperationId = paymentOperationId;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getProposalId() { return proposalId; }
    public String getInvoiceId() { return invoiceId; }
    public int getLineNo() { return lineNo; }
    public BigDecimal getAmount() { return amount; }
    public String getSupplierPaymentId() { return supplierPaymentId; }
    public String getPaymentOperationId() { return paymentOperationId; }
    public long getCreatedAt() { return createdAt; }
}
