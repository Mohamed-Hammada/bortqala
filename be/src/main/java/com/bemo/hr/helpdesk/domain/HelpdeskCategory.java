package com.bemo.hr.helpdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "helpdesk_categories")
public class HelpdeskCategory {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(nullable = false, length = 200)
    private String nameAr;
    @Column(nullable = false, length = 200)
    private String nameEn;
    @Column(name = "sla_first_response_hours", nullable = false)
    private int slaFirstResponseHours = 8;
    @Column(name = "sla_resolution_hours", nullable = false)
    private int slaResolutionHours = 48;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    private Long version;

    protected HelpdeskCategory() {}

    public HelpdeskCategory(String appId, String nameAr, String nameEn,
                            int slaFirstResponseHours, int slaResolutionHours) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.slaFirstResponseHours = slaFirstResponseHours;
        this.slaResolutionHours = slaResolutionHours;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getNameAr() { return nameAr; }
    public String getNameEn() { return nameEn; }
    public int getSlaFirstResponseHours() { return slaFirstResponseHours; }
    public int getSlaResolutionHours() { return slaResolutionHours; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }

    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public void setSlaFirstResponseHours(int h) { this.slaFirstResponseHours = h; }
    public void setSlaResolutionHours(int h) { this.slaResolutionHours = h; }
    public void setActive(boolean active) { this.active = active; }
}
