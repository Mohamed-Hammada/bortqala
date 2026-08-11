package com.bemo.hr.operations.domain;

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
@Table(name = "item_lots_serials")
public class ItemLotSerial {

    public enum Status {
        AVAILABLE, QUARANTINED, EXPIRED, BLOCKED
    }

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

    protected ItemLotSerial() {}

    public ItemLotSerial(String itemId, String lotNumber, String serialNumber, LocalDate expirationDate, LocalDate manufactureDate) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.lotNumber = lotNumber == null ? null : lotNumber.strip();
        this.serialNumber = serialNumber == null ? null : serialNumber.strip();
        this.expirationDate = expirationDate;
        this.manufactureDate = manufactureDate;
        this.status = Status.AVAILABLE;
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
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getItemId() { return itemId; }
    public String getLotNumber() { return lotNumber; }
    public String getSerialNumber() { return serialNumber; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public LocalDate getManufactureDate() { return manufactureDate; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
