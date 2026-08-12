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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vendor_payment_proposals")
public class VendorPaymentProposal {

    public enum Status {
        PROPOSED, APPROVED, REJECTED, EXECUTED
    }

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

    protected VendorPaymentProposal() {}

    public VendorPaymentProposal(String supplierId, String invoiceId, BigDecimal proposedAmount, LocalDate dueDate) {
        this.id = UUID.randomUUID().toString();
        this.proposalNumber = "PROP-" + System.currentTimeMillis();
        this.supplierId = supplierId;
        this.invoiceId = invoiceId;
        this.proposedAmount = proposedAmount;
        this.dueDate = dueDate;
        this.status = Status.PROPOSED;
    }

    public void approve() {
        this.status = Status.APPROVED;
    }

    public void execute() {
        this.status = Status.EXECUTED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getProposalNumber() { return proposalNumber; }
    public String getSupplierId() { return supplierId; }
    public String getInvoiceId() { return invoiceId; }
    public BigDecimal getProposedAmount() { return proposedAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
