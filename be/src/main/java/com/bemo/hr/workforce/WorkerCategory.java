package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "worker_categories")
@Getter
public class WorkerCategory {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 500) private String description;
    @Column(name = "default_daily_rate", precision = 12, scale = 2, nullable = false) private BigDecimal defaultDailyRate;
    @Column(name = "standard_daily_hours", precision = 4, scale = 2, nullable = false) private BigDecimal standardDailyHours;
    @Column(name = "default_settlement_cycle", nullable = false, length = 50) private String defaultSettlementCycle;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkerCategory() { }

    public WorkerCategory(String code, String name, String description, BigDecimal defaultDailyRate,
                          BigDecimal standardDailyHours, String defaultSettlementCycle, String status) {
        this.id = UUID.randomUUID().toString();
        update(code, name, description, defaultDailyRate, standardDailyHours, defaultSettlementCycle, status);
    }

    public void update(String code, String name, String description, BigDecimal defaultDailyRate,
                       BigDecimal standardDailyHours, String defaultSettlementCycle, String status) {
        this.code = code != null ? code.strip().toUpperCase() : "CAT-" + UUID.randomUUID().toString().substring(0, 4);
        this.name = name != null ? name.strip() : "";
        this.description = description != null && !description.isBlank() ? description.strip() : null;
        this.defaultDailyRate = defaultDailyRate != null ? defaultDailyRate : BigDecimal.ZERO;
        this.standardDailyHours = standardDailyHours != null ? standardDailyHours : new BigDecimal("8.0");
        this.defaultSettlementCycle = defaultSettlementCycle != null ? defaultSettlementCycle : "HALF_MONTH";
        this.status = status != null ? status.strip().toUpperCase() : "ACTIVE";
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
