package com.bemo.hr.trade.sales.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_credit_notes")
public class CustomerCreditNote {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "credit_note_number", nullable = false, length = 50)
    private String creditNoteNumber;
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    @Column(name = "invoice_id", nullable = false, length = 36)
    private String invoiceId;
    @Column(name = "sales_order_id", nullable = false, length = 36)
    private String salesOrderId;
    @Column(name = "delivery_id", nullable = false, length = 36)
    private String deliveryId;
    @Column(name = "return_id", nullable = false, length = 36)
    private String returnId;
    @Column(name = "credit_date", nullable = false)
    private LocalDate creditDate;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected CustomerCreditNote() {
    }

    public CustomerCreditNote(String number, String customerId, String invoiceId, String salesOrderId,
                              String deliveryId, String returnId, LocalDate date, String currencyCode,
                              BigDecimal amount, String operationId, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.creditNoteNumber = number;
        this.customerId = customerId;
        this.invoiceId = invoiceId;
        this.salesOrderId = salesOrderId;
        this.deliveryId = deliveryId;
        this.returnId = returnId;
        this.creditDate = date;
        this.currencyCode = currencyCode;
        this.amount = amount;
        this.operationId = operationId;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getCreditNoteNumber() {
        return creditNoteNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getSalesOrderId() {
        return salesOrderId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public String getReturnId() {
        return returnId;
    }

    public LocalDate getCreditDate() {
        return creditDate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getOperationId() {
        return operationId;
    }
}
