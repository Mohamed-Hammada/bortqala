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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_return_headers")
public class CustomerReturnHeader {

    public enum Status {
        DRAFT, RECEIVED, APPROVED, REFUNDED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "return_number", nullable = false, length = 50)
    private String returnNumber;

    @Column(name = "sales_order_id", nullable = false, length = 36)
    private String salesOrderId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(length = 500)
    private String reason;

    @Column(name = "delivery_id", nullable = false, length = 36)
    private String deliveryId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    @Column(name = "credit_note_id", length = 36)
    private String creditNoteId;

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

    protected CustomerReturnHeader() {}

    public CustomerReturnHeader(String returnNumber, String salesOrderId, String customerId, LocalDate returnDate,
                                String reason, String deliveryId, String warehouseId, String operationId) {
        this.id = UUID.randomUUID().toString();
        this.returnNumber = returnNumber;
        this.salesOrderId = salesOrderId;
        this.customerId = customerId;
        this.returnDate = returnDate;
        this.reason = reason;
        this.deliveryId = deliveryId;
        this.warehouseId = warehouseId;
        this.operationId = operationId;
        this.status = Status.DRAFT;
    }

    public void receive() {
        this.status = Status.RECEIVED;
    }

    public void approve() {
        this.status = Status.APPROVED;
    }

    public void refund() {
        this.status = Status.REFUNDED;
    }

    public void linkCreditNote(String creditNoteId) { this.creditNoteId = creditNoteId; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getReturnNumber() { return returnNumber; }
    public String getSalesOrderId() { return salesOrderId; }
    public String getCustomerId() { return customerId; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getReason() { return reason; }
    public String getDeliveryId() { return deliveryId; }
    public String getWarehouseId() { return warehouseId; }
    public String getOperationId() { return operationId; }
    public String getCreditNoteId() { return creditNoteId; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
