package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workforce_settlement_periods")
@Getter
public class WorkforceSettlementPeriod {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "period_code", nullable = false, length = 50) private String periodCode;
    @Column(name = "start_date", nullable = false, length = 10) private String startDate;
    @Column(name = "end_date", nullable = false, length = 10) private String endDate;
    @Column(name = "cycle_type", nullable = false, length = 30) private String cycleType;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkforceSettlementPeriod() { }

    public WorkforceSettlementPeriod(String periodCode, String startDate, String endDate, String cycleType, String status) {
        this.id = UUID.randomUUID().toString();
        this.periodCode = periodCode != null ? periodCode.strip().toUpperCase() : "SETTL-" + UUID.randomUUID().toString().substring(0, 6);
        this.startDate = startDate;
        this.endDate = endDate;
        this.cycleType = cycleType != null ? cycleType.strip().toUpperCase() : "HALF_MONTH";
        this.status = status != null ? status.strip().toUpperCase() : "DRAFT";
    }

    public void setStatus(String status) {
        this.status = status.strip().toUpperCase();
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
