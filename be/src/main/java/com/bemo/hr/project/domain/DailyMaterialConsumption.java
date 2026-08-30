package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "daily_material_consumptions")
public class DailyMaterialConsumption {

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

    @Column(name = "material_name", length = 255, nullable = false)
    private String materialName;

    @Column(name = "unit_of_measure", length = 32, nullable = false)
    private String unitOfMeasure;

    @Column(name = "quantity_used", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantityUsed;

    @Column(name = "delivery_note_number", length = 64)
    private String deliveryNoteNumber;

    @Column(name = "supplier_party_id", length = 36)
    private String supplierPartyId;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyMaterialConsumption() {
    }

    public DailyMaterialConsumption(String dailyReportId, String wbsNodeId, String materialName,
                                    String unitOfMeasure, BigDecimal quantityUsed, String deliveryNoteNumber,
                                    String supplierPartyId, String notes) {
        this.id = UUID.randomUUID().toString();
        this.dailyReportId = Objects.requireNonNull(dailyReportId, "dailyReportId must not be null");
        this.wbsNodeId = wbsNodeId;
        this.materialName = Objects.requireNonNull(materialName, "materialName must not be null");
        this.unitOfMeasure = Objects.requireNonNull(unitOfMeasure, "unitOfMeasure must not be null");
        this.quantityUsed = quantityUsed != null ? quantityUsed : BigDecimal.ZERO;
        this.deliveryNoteNumber = deliveryNoteNumber;
        this.supplierPartyId = supplierPartyId;
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

    public String getMaterialName() {
        return materialName;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public BigDecimal getQuantityUsed() {
        return quantityUsed;
    }

    public String getDeliveryNoteNumber() {
        return deliveryNoteNumber;
    }

    public String getSupplierPartyId() {
        return supplierPartyId;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
