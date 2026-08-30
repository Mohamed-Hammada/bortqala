package com.bemo.hr.compliance.eta.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "eta_item_code_mappings")
public class EtaItemCodeMapping {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "item_id", nullable = false, length = 36)
    private String itemId;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "code_type", nullable = false, length = 30)
    private String codeType; // EGS, GS1

    @Column(name = "item_code_value", nullable = false, length = 100)
    private String itemCodeValue; // e.g. EG-113322445-001 or 6221123456789

    @Column(name = "description_ar", length = 255)
    private String descriptionAr;

    @Column(name = "description_en", length = 255)
    private String descriptionEn;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected EtaItemCodeMapping() {
    }

    public EtaItemCodeMapping(String itemId, String itemCode, String codeType, String itemCodeValue, String descriptionAr, String descriptionEn) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.codeType = codeType;
        this.itemCodeValue = itemCodeValue;
        this.descriptionAr = descriptionAr;
        this.descriptionEn = descriptionEn;
        this.active = true;
    }

    public void update(String codeType, String itemCodeValue, String descriptionAr, String descriptionEn, boolean active) {
        this.codeType = codeType;
        this.itemCodeValue = itemCodeValue;
        this.descriptionAr = descriptionAr;
        this.descriptionEn = descriptionEn;
        this.active = active;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getCodeType() {
        return codeType;
    }

    public String getItemCodeValue() {
        return itemCodeValue;
    }

    public String getDescriptionAr() {
        return descriptionAr;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public boolean isActive() {
        return active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
