package com.bemo.hr.trade.export.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "lot_treatment_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"app_id", "lot_reference", "chemical", "treatment_date"})
})
public class ComplianceRegister {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "lot_reference", nullable = false, length = 100)
    private String lotReference;

    @Column(name = "chemical", nullable = false, length = 200)
    private String chemical;

    @Column(name = "dose", length = 100)
    private String dose;

    @Column(name = "treatment_date", nullable = false)
    private LocalDate treatmentDate;

    @Column(name = "pre_harvest_interval_days", nullable = false)
    private int preHarvestIntervalDays;

    @Column(name = "treated_by", length = 200)
    private String treatedBy;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected ComplianceRegister() {
    }

    public ComplianceRegister(String lotReference, String chemical, LocalDate treatmentDate,
                              int preHarvestIntervalDays) {
        this.id = UUID.randomUUID().toString();
        this.lotReference = lotReference.strip();
        this.chemical = chemical.strip();
        this.treatmentDate = treatmentDate;
        this.preHarvestIntervalDays = preHarvestIntervalDays;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
    }

    public LocalDate earliestSafePickup() {
        return treatmentDate.plusDays(preHarvestIntervalDays);
    }

    public boolean isViolation(LocalDate pickupDate) {
        return pickupDate.isBefore(earliestSafePickup());
    }

    public long daysUntilSafe(LocalDate pickupDate) {
        return ChronoUnit.DAYS.between(pickupDate, earliestSafePickup());
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getLotReference() { return lotReference; }
    public void setLotReference(String v) { this.lotReference = v; }
    public String getChemical() { return chemical; }
    public void setChemical(String v) { this.chemical = v; }
    public String getDose() { return dose; }
    public void setDose(String v) { this.dose = v; }
    public LocalDate getTreatmentDate() { return treatmentDate; }
    public void setTreatmentDate(LocalDate v) { this.treatmentDate = v; }
    public int getPreHarvestIntervalDays() { return preHarvestIntervalDays; }
    public void setPreHarvestIntervalDays(int v) { this.preHarvestIntervalDays = v; }
    public String getTreatedBy() { return treatedBy; }
    public void setTreatedBy(String v) { this.treatedBy = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public long getCreatedAt() { return createdAt; }
}
