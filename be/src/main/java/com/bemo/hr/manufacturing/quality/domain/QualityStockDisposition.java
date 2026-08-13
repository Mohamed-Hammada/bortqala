package com.bemo.hr.manufacturing.quality.domain;

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
import java.util.UUID;

@Entity
@Table(name = "quality_stock_dispositions")
public class QualityStockDisposition {

    public enum Status {
        DISPOSED
    }

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "inspection_id", nullable = false, length = 36)
    private String inspectionId;

    @Column(name = "disposition_type", nullable = false, length = 50)
    private String dispositionType;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DISPOSED;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected QualityStockDisposition() {}

    public QualityStockDisposition(String inspectionId, String dispositionType, BigDecimal quantity, String reason) {
        this.id = UUID.randomUUID().toString();
        this.inspectionId = inspectionId;
        this.dispositionType = dispositionType;
        this.quantity = quantity;
        this.reason = reason;
        this.status = Status.DISPOSED;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getInspectionId() { return inspectionId; }
    public String getDispositionType() { return dispositionType; }
    public BigDecimal getQuantity() { return quantity; }
    public String getReason() { return reason; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
