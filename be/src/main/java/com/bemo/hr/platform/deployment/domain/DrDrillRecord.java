package com.bemo.hr.platform.deployment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "dr_drill_records")
public class DrDrillRecord {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "drill_name", length = 120, nullable = false)
    private String drillName;

    @Column(name = "target_rpo_minutes", nullable = false)
    private int targetRpoMinutes;

    @Column(name = "target_rto_minutes", nullable = false)
    private int targetRtoMinutes;

    @Column(name = "actual_rpo_minutes", nullable = false)
    private int actualRpoMinutes;

    @Column(name = "actual_rto_minutes", nullable = false)
    private int actualRtoMinutes;

    @Column(name = "status", length = 32, nullable = false)
    private String status; // SUCCESS, DEGRADED, FAILED

    @Column(name = "drill_details_json", columnDefinition = "TEXT")
    private String drillDetailsJson;

    @Column(name = "conducted_by", length = 64, nullable = false)
    private String conductedBy;

    @Column(name = "conducted_at", nullable = false)
    private long conductedAt;

    protected DrDrillRecord() {}

    public DrDrillRecord(
            String drillName,
            int targetRpoMinutes,
            int targetRtoMinutes,
            int actualRpoMinutes,
            int actualRtoMinutes,
            String status,
            String drillDetailsJson,
            String conductedBy,
            long conductedAt) {
        this.id = UUID.randomUUID().toString();
        this.drillName = Objects.requireNonNull(drillName);
        this.targetRpoMinutes = targetRpoMinutes;
        this.targetRtoMinutes = targetRtoMinutes;
        this.actualRpoMinutes = actualRpoMinutes;
        this.actualRtoMinutes = actualRtoMinutes;
        this.status = Objects.requireNonNull(status);
        this.drillDetailsJson = drillDetailsJson;
        this.conductedBy = Objects.requireNonNull(conductedBy);
        this.conductedAt = conductedAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getDrillName() { return drillName; }
    public int getTargetRpoMinutes() { return targetRpoMinutes; }
    public int getTargetRtoMinutes() { return targetRtoMinutes; }
    public int getActualRpoMinutes() { return actualRpoMinutes; }
    public int getActualRtoMinutes() { return actualRtoMinutes; }
    public String getStatus() { return status; }
    public String getDrillDetailsJson() { return drillDetailsJson; }
    public String getConductedBy() { return conductedBy; }
    public long getConductedAt() { return conductedAt; }
}
