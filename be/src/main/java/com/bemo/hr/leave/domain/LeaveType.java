package com.bemo.hr.leave.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "leave_types")
public class LeaveType {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name_ar", nullable = false, length = 200)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "is_paid", nullable = false)
    private boolean paid;

    @Column(name = "requires_attachment", nullable = false)
    private boolean requiresAttachment;

    @Column(name = "max_consecutive_days", nullable = false)
    private int maxConsecutiveDays;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected LeaveType() {
    }

    public LeaveType(String code, String nameAr, String nameEn, boolean paid, boolean requiresAttachment, int maxConsecutiveDays) {
        this.id = UUID.randomUUID().toString();
        this.code = code;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.paid = paid;
        this.requiresAttachment = requiresAttachment;
        this.maxConsecutiveDays = maxConsecutiveDays > 0 ? maxConsecutiveDays : 30;
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

    public String getCode() {
        return code;
    }

    public String getNameAr() {
        return nameAr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public boolean isPaid() {
        return paid;
    }

    public boolean isRequiresAttachment() {
        return requiresAttachment;
    }

    public int getMaxConsecutiveDays() {
        return maxConsecutiveDays;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
