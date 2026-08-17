package com.bemo.hr.operations.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "item_lots_serials")
public class ItemLotSerial {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;
    @Column(name = "lot_number", length = 50)
    private String lotNumber;
    @Column(name = "serial_number", length = 50)
    private String serialNumber;
    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;
    @Column(name = "receipt_reference", length = 100)
    private String receiptReference;
    @Column(name = "issue_reference", length = 100)
    private String issueReference;
    @Column(name = "return_reference", length = 100)
    private String returnReference;
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AVAILABLE;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected ItemLotSerial() {
    }

    public ItemLotSerial(String itemId, String lotNumber, String serialNumber, LocalDate expirationDate, LocalDate manufactureDate) {
        this(itemId, null, lotNumber, serialNumber, serialNumber == null || serialNumber.isBlank() ? BigDecimal.ZERO : BigDecimal.ONE,
                null, expirationDate, manufactureDate);
    }

    public ItemLotSerial(String itemId, String warehouseId, String lotNumber, String serialNumber, BigDecimal quantity,
                         String receiptReference, LocalDate expirationDate, LocalDate manufactureDate) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.warehouseId = normalize(warehouseId);
        this.lotNumber = lotNumber == null ? null : lotNumber.strip();
        this.serialNumber = serialNumber == null ? null : serialNumber.strip();
        this.quantity = quantity == null ? BigDecimal.ZERO : quantity;
        this.receiptReference = normalize(receiptReference);
        this.expirationDate = expirationDate;
        this.manufactureDate = manufactureDate;
        this.status = Status.AVAILABLE;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void requirePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0)
            throw new IllegalArgumentException("Lot/serial quantity must be positive");
    }

    public void issue(BigDecimal issuedQuantity, String documentReference) {
        requirePositive(issuedQuantity);
        if (status != Status.AVAILABLE || quantity.compareTo(issuedQuantity) < 0) {
            throw new IllegalStateException("Lot/serial is unavailable or has insufficient quantity");
        }
        if (serialNumber != null && issuedQuantity.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("A serial-controlled issue quantity must equal one");
        }
        quantity = quantity.subtract(issuedQuantity);
        issueReference = normalize(documentReference);
    }

    public void receiveReturn(BigDecimal returnedQuantity, String documentReference) {
        requirePositive(returnedQuantity);
        if (serialNumber != null && returnedQuantity.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("A serial-controlled return quantity must equal one");
        }
        quantity = quantity.add(returnedQuantity);
        returnReference = normalize(documentReference);
        status = Status.AVAILABLE;
    }

    public void quarantine() {
        this.status = Status.QUARANTINED;
    }

    public void block() {
        this.status = Status.BLOCKED;
    }

    public void checkExpired(LocalDate currentDate) {
        if (expirationDate != null && expirationDate.isBefore(currentDate)) {
            this.status = Status.EXPIRED;
        }
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

    public String getItemId() {
        return itemId;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getReceiptReference() {
        return receiptReference;
    }

    public String getIssueReference() {
        return issueReference;
    }

    public String getReturnReference() {
        return returnReference;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
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
        AVAILABLE, QUARANTINED, EXPIRED, BLOCKED
    }
}
