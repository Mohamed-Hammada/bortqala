package com.bemo.hr.workforce;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workers")
@Getter
public class Worker {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;
    @Column(name = "contractor_id", nullable = false, length = 36)
    private String contractorId;
    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;
    @Column(name = "default_daily_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal defaultDailyRate;
    @Column(name = "standard_daily_hours", precision = 4, scale = 2, nullable = false)
    private BigDecimal standardDailyHours;
    @Column(name = "branch_id", length = 36)
    private String branchId;
    @Column(name = "attendance_mode", nullable = false, length = 30)
    private String attendanceMode;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(length = 50)
    private String phone;
    @Column(name = "national_id", length = 50)
    private String nationalId;
    @Column(length = 1000)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Worker() {
    }

    public Worker(String code, String fullName, String contractorId, String categoryId,
                  BigDecimal defaultDailyRate, BigDecimal standardDailyHours, String branchId,
                  String attendanceMode, String status, String phone, String nationalId, String notes) {
        this.id = UUID.randomUUID().toString();
        update(code, fullName, contractorId, categoryId, defaultDailyRate, standardDailyHours,
                branchId, attendanceMode, status, phone, nationalId, notes);
    }

    public void update(String code, String fullName, String contractorId, String categoryId,
                       BigDecimal defaultDailyRate, BigDecimal standardDailyHours, String branchId,
                       String attendanceMode, String status, String phone, String nationalId, String notes) {
        this.code = code != null ? code.strip().toUpperCase() : "WRK-" + UUID.randomUUID().toString().substring(0, 6);
        this.fullName = fullName != null ? fullName.strip() : "";
        this.contractorId = contractorId;
        this.categoryId = categoryId;
        this.defaultDailyRate = defaultDailyRate != null ? defaultDailyRate : BigDecimal.ZERO;
        this.standardDailyHours = standardDailyHours != null ? standardDailyHours : new BigDecimal("8.0");
        this.branchId = branchId;
        this.attendanceMode = attendanceMode != null ? attendanceMode.strip().toUpperCase() : "MANUAL";
        this.status = status != null ? status.strip().toUpperCase() : "ACTIVE";
        this.phone = phone != null && !phone.isBlank() ? phone.strip() : null;
        this.nationalId = nationalId != null && !nationalId.isBlank() ? nationalId.strip() : null;
        this.notes = notes != null && !notes.isBlank() ? notes.strip() : null;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
