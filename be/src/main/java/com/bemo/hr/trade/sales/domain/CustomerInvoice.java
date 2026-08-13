package com.bemo.hr.trade.sales.domain;

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
@Table(name = "customer_invoices")
public class CustomerInvoice {

    public enum Status {
        DRAFT, ISSUED, PAID, POSTED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "sales_order_id", length = 36)
    private String salesOrderId;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "outstanding_amount", precision = 15, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(name = "delivered_quantity", precision = 15, scale = 4)
    private BigDecimal deliveredQuantity;

    @Column(name = "invoiced_amount", precision = 15, scale = 2)
    private BigDecimal invoicedAmount;

    @Column(name = "cogs_amount", precision = 15, scale = 2)
    private BigDecimal cogsAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CustomerInvoice() {}

    public CustomerInvoice(String invoiceNumber, String customerId, String salesOrderId, LocalDate invoiceDate, LocalDate dueDate, String currencyCode, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.invoiceNumber = invoiceNumber;
        this.customerId = customerId;
        this.salesOrderId = salesOrderId;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.currencyCode = currencyCode;
        this.amount = amount;
        this.outstandingAmount = amount;
        this.status = Status.DRAFT;
    }

    public CustomerInvoice(String salesOrderId, BigDecimal deliveredQuantity, BigDecimal unitPrice, BigDecimal unitCogs) {
        this.id = UUID.randomUUID().toString();
        this.invoiceNumber = "INV-" + System.currentTimeMillis();
        this.salesOrderId = salesOrderId;
        this.deliveredQuantity = deliveredQuantity;
        this.invoicedAmount = deliveredQuantity.multiply(unitPrice);
        this.cogsAmount = deliveredQuantity.multiply(unitCogs);
        this.amount = this.invoicedAmount;
        this.outstandingAmount = this.invoicedAmount;
        this.invoiceDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30);
        this.currencyCode = "EGP";
        this.status = Status.POSTED;
    }

    public void issue(String actor) {
        this.status = Status.ISSUED;
    }

    public void allocate(BigDecimal allocationAmount) {
        if (allocationAmount.compareTo(this.outstandingAmount) > 0) {
            throw new IllegalArgumentException("Allocation exceeds outstanding amount");
        }
        this.outstandingAmount = this.outstandingAmount.subtract(allocationAmount);
        if (this.outstandingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = Status.PAID;
        }
    }

    public void applyCredit(BigDecimal creditAmount) {
        if (creditAmount == null || creditAmount.signum() <= 0 || creditAmount.compareTo(amount) > 0) {
            throw new IllegalArgumentException("Credit amount is invalid");
        }
        amount = amount.subtract(creditAmount);
        outstandingAmount = outstandingAmount.subtract(creditAmount).max(BigDecimal.ZERO);
        status = outstandingAmount.signum() == 0 ? Status.PAID : Status.ISSUED;
    }

    public boolean overdue(LocalDate asOf) {
        return this.status != Status.PAID && this.dueDate != null && this.dueDate.isBefore(asOf);
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getCustomerId() { return customerId; }
    public String getSalesOrderId() { return salesOrderId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public String getCurrencyCode() { return currencyCode; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public BigDecimal getDeliveredQuantity() { return deliveredQuantity; }
    public BigDecimal getInvoicedAmount() { return invoicedAmount; }
    public BigDecimal getCogsAmount() { return cogsAmount; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
