package com.bemo.hr.operations;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unit_conversions", uniqueConstraints = @UniqueConstraint(columnNames = {"app_id", "from_uom_id", "to_uom_id"}))
public class UnitConversion {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "from_uom_id", nullable = false, length = 36)
    private String fromUomId;
    @Column(name = "to_uom_id", nullable = false, length = 36)
    private String toUomId;
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal factor;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    protected UnitConversion() {
    }

    public UnitConversion(String fromUomId, String toUomId, BigDecimal factor, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.fromUomId = fromUomId;
        this.toUomId = toUomId;
        this.factor = factor;
        this.createdBy = createdBy;
    }

    @jakarta.persistence.PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getFromUomId() {
        return fromUomId;
    }

    public String getToUomId() {
        return toUomId;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
