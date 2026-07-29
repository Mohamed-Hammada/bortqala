package com.bemo.hr.workforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workforce_settlement_issues")
@Getter
public class WorkforceSettlementIssue {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(name = "period_id", nullable = false, length = 36) private String periodId;
    @Column(name = "calculation_version", nullable = false) private int calculationVersion;
    @Column(name = "worker_id", length = 36) private String workerId;
    @Column(name = "worker_name", length = 160) private String workerName;
    @Column(nullable = false, length = 20) private String severity;
    @Column(nullable = false, length = 60) private String code;
    @Column(nullable = false, length = 500) private String message;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected WorkforceSettlementIssue() { }

    public WorkforceSettlementIssue(String periodId, int calculationVersion, String workerId,
                                    String workerName, String severity, String code, String message) {
        this.id = UUID.randomUUID().toString();
        this.periodId = periodId;
        this.calculationVersion = calculationVersion;
        this.workerId = workerId;
        this.workerName = workerName;
        this.severity = severity;
        this.code = code;
        this.message = message;
    }

    @PrePersist void prePersist() { createdAt = Instant.now(); }
}
