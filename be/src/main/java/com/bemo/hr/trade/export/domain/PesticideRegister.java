package com.bemo.hr.trade.export.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pesticide_registers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"app_id", "chemical_name"})
})
public class PesticideRegister {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "chemical_name", nullable = false, length = 200)
    private String chemicalName;

    @Column(name = "active_ingredient", length = 200)
    private String activeIngredient;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "mrl_mg_per_kg", precision = 10, scale = 3)
    private BigDecimal mrlMgPerKg;

    @Column(name = "max_dose_per_ha", length = 100)
    private String maxDosePerHa;

    @Column(name = "pre_harvest_interval_days")
    private Integer preHarvestIntervalDays;

    @Column(name = "crop_authorized", length = 500)
    private String cropAuthorized;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected PesticideRegister() {
    }

    public PesticideRegister(String chemicalName) {
        this.id = UUID.randomUUID().toString();
        this.chemicalName = chemicalName.strip();
        this.status = "ACTIVE";
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getChemicalName() { return chemicalName; }
    public void setChemicalName(String v) { this.chemicalName = v; }
    public String getActiveIngredient() { return activeIngredient; }
    public void setActiveIngredient(String v) { this.activeIngredient = v; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String v) { this.registrationNumber = v; }
    public BigDecimal getMrlMgPerKg() { return mrlMgPerKg; }
    public void setMrlMgPerKg(BigDecimal v) { this.mrlMgPerKg = v; }
    public String getMaxDosePerHa() { return maxDosePerHa; }
    public void setMaxDosePerHa(String v) { this.maxDosePerHa = v; }
    public Integer getPreHarvestIntervalDays() { return preHarvestIntervalDays; }
    public void setPreHarvestIntervalDays(Integer v) { this.preHarvestIntervalDays = v; }
    public String getCropAuthorized() { return cropAuthorized; }
    public void setCropAuthorized(String v) { this.cropAuthorized = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
