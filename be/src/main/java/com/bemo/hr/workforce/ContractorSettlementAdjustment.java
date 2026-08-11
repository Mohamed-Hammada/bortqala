package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contractor_settlement_adjustments")
@Getter
public class ContractorSettlementAdjustment {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "settlement_id", nullable = false, length = 36) private String settlementId;
    @Column(name = "adjustment_type", nullable = false, length = 50) private String adjustmentType;
    @Column(length = 500) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(length = 500) private String reason;
    @Column(name = "created_by", length = 160) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ContractorSettlementAdjustment() { }

    public ContractorSettlementAdjustment(String settlementId, String adjustmentType, String description,
                                          BigDecimal amount, String reason, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.settlementId = settlementId;
        this.adjustmentType = adjustmentType != null ? adjustmentType.strip().toUpperCase() : "OTHER";
        this.description = description;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
