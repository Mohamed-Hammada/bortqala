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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sales_orders")
public class SalesOrder {

    public enum Status {
        DRAFT,
        CONFIRMED,
        DELIVERED,
        CANCELLED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "so_number", nullable = false, length = 50)
    private String soNumber;

    @Column(name = "so_date", nullable = false)
    private LocalDate soDate;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "quotation_id", length = 36)
    private String quotationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "EGP";

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SalesOrder() {}

    public SalesOrder(String soNumber, LocalDate soDate, String customerId, String quotationId, BigDecimal totalAmount) {
        this.id = UUID.randomUUID().toString();
        this.soNumber = soNumber.strip();
        this.soDate = soDate;
        this.customerId = customerId;
        this.quotationId = quotationId == null || quotationId.isBlank() ? null : quotationId.strip();
        this.status = Status.DRAFT;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void configureFulfillment(String warehouseId, String currencyCode) {
        if (status != Status.DRAFT) throw new IllegalStateException("Only draft orders can change fulfillment settings");
        this.warehouseId = warehouseId == null || warehouseId.isBlank() ? null : warehouseId.strip();
        this.currencyCode = currencyCode == null || currencyCode.isBlank() ? "EGP" : currencyCode.strip().toUpperCase();
    }

    public void replaceDerivedTotal(BigDecimal totalAmount) {
        if (status != Status.DRAFT) throw new IllegalStateException("Only draft orders can change their derived total");
        this.totalAmount = totalAmount;
    }

    public void confirm() {
        if (status != Status.DRAFT) throw new IllegalStateException("Only draft orders can be confirmed");
        status = Status.CONFIRMED;
    }

    public void deliver() {
        if (status != Status.CONFIRMED) throw new IllegalStateException("Only confirmed orders can be delivered");
        status = Status.DELIVERED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getSoNumber() { return soNumber; }
    public LocalDate getSoDate() { return soDate; }
    public String getCustomerId() { return customerId; }
    public String getQuotationId() { return quotationId; }
    public Status getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getWarehouseId() { return warehouseId; }
    public String getCurrencyCode() { return currencyCode; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
