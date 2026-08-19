package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "daily_equipment_logs")
public class DailyEquipmentLog {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "daily_report_id", length = 36, nullable = false)
    private String dailyReportId;

    @Column(name = "wbs_node_id", length = 36)
    private String wbsNodeId;

    @Column(name = "equipment_type", length = 100, nullable = false)
    private String equipmentType;

    @Column(name = "equipment_code", length = 64)
    private String equipmentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private EquipmentSiteStatus status;

    @Column(name = "hours_operated", precision = 6, scale = 2, nullable = false)
    private BigDecimal hoursOperated;

    @Column(name = "hours_idle", precision = 6, scale = 2, nullable = false)
    private BigDecimal hoursIdle;

    @Column(name = "fuel_consumed_liters", precision = 10, scale = 2)
    private BigDecimal fuelConsumedLiters;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyEquipmentLog() {
    }

    public DailyEquipmentLog(String dailyReportId, String wbsNodeId, String equipmentType,
                             String equipmentCode, EquipmentSiteStatus status, BigDecimal hoursOperated,
                             BigDecimal hoursIdle, BigDecimal fuelConsumedLiters, String operatorName,
                             String notes) {
        this.id = UUID.randomUUID().toString();
        this.dailyReportId = Objects.requireNonNull(dailyReportId, "dailyReportId must not be null");
        this.wbsNodeId = wbsNodeId;
        this.equipmentType = Objects.requireNonNull(equipmentType, "equipmentType must not be null");
        this.equipmentCode = equipmentCode;
        this.status = status != null ? status : EquipmentSiteStatus.WORKING;
        this.hoursOperated = hoursOperated != null ? hoursOperated : BigDecimal.ZERO;
        this.hoursIdle = hoursIdle != null ? hoursIdle : BigDecimal.ZERO;
        this.fuelConsumedLiters = fuelConsumedLiters != null ? fuelConsumedLiters : BigDecimal.ZERO;
        this.operatorName = operatorName;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    // ─── Getters ─────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDailyReportId() {
        return dailyReportId;
    }

    public String getWbsNodeId() {
        return wbsNodeId;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public EquipmentSiteStatus getStatus() {
        return status;
    }

    public BigDecimal getHoursOperated() {
        return hoursOperated;
    }

    public BigDecimal getHoursIdle() {
        return hoursIdle;
    }

    public BigDecimal getFuelConsumedLiters() {
        return fuelConsumedLiters;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
