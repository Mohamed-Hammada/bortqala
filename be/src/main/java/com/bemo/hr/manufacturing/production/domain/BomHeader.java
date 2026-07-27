package com.bemo.hr.manufacturing.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "boms")
public class BomHeader {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "bom_code", nullable = false, length = 50)
    private String bomCode;

    @Column(name = "finished_good_name", nullable = false, length = 255)
    private String finishedGoodName;

    @Column(name = "yield_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal yieldQuantity;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected BomHeader() {}

    public BomHeader(String bomCode, String finishedGoodName, BigDecimal yieldQuantity, String notes, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(bomCode, finishedGoodName, yieldQuantity, notes, active);
    }

    public void update(String bomCode, String finishedGoodName, BigDecimal yieldQuantity, String notes, boolean active) {
        this.bomCode = bomCode.strip();
        this.finishedGoodName = finishedGoodName.strip();
        this.yieldQuantity = yieldQuantity == null ? BigDecimal.ONE : yieldQuantity;
        this.notes = notes == null ? null : notes.strip();
        this.active = active;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getBomCode() { return bomCode; }
    public String getFinishedGoodName() { return finishedGoodName; }
    public BigDecimal getYieldQuantity() { return yieldQuantity; }
    public String getNotes() { return notes; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
