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
@Table(name = "sales_delivery_headers")
public class SalesDeliveryHeader {

    public enum Status {
        DRAFT, SHIPPED, DELIVERED, CANCELLED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "delivery_number", nullable = false, length = 50)
    private String deliveryNumber;

    @Column(name = "sales_order_id", nullable = false, length = 36)
    private String salesOrderId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

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

    protected SalesDeliveryHeader() {}

    public SalesDeliveryHeader(String deliveryNumber, String salesOrderId, String customerId, LocalDate deliveryDate,
                               String warehouseId, String operationId) {
        this.id = UUID.randomUUID().toString();
        this.deliveryNumber = deliveryNumber;
        this.salesOrderId = salesOrderId;
        this.customerId = customerId;
        this.deliveryDate = deliveryDate;
        this.warehouseId = warehouseId;
        this.operationId = operationId;
        this.status = Status.DRAFT;
    }

    public void ship() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT deliveries can be shipped");
        }
        this.status = Status.SHIPPED;
    }

    public void deliver() {
        if (this.status != Status.SHIPPED) {
            throw new IllegalStateException("Only SHIPPED deliveries can be delivered");
        }
        this.status = Status.DELIVERED;
    }

    public void linkInvoice(String invoiceId) { this.invoiceId = invoiceId; }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getDeliveryNumber() { return deliveryNumber; }
    public String getSalesOrderId() { return salesOrderId; }
    public String getCustomerId() { return customerId; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public String getWarehouseId() { return warehouseId; }
    public String getOperationId() { return operationId; }
    public String getInvoiceId() { return invoiceId; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
