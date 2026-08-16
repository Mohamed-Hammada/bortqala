package com.bemo.hr.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered biometric punch source. A physical device ({@code DEVICE}) or a
 * file-import origin ({@code FILE_DEVICE}) referenced by punch_records and
 * import_batches, so deduplication is stable even when a display name changes.
 */
@Entity
@Table(name = "biometric_sources")
public class BiometricSource {
    public enum SourceType {
        DEVICE,
        FILE_DEVICE
    }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "normalized_code", nullable = false, length = 150)
    private String normalizedCode;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "auto_create_employees", nullable = false)
    private boolean autoCreateEmployees;
    @Column(name = "auto_create_category_id")
    private String autoCreateCategoryId;
    @Column(name = "auto_create_employment_type", nullable = false, length = 20)
    private String autoCreateEmploymentType = "FIXED";
    @Column(name = "auto_create_active_from_mode", nullable = false, length = 20)
    private String autoCreateActiveFromMode = "FIRST_PUNCH";
    @Column(name = "auto_create_employee_active", nullable = false)
    private boolean autoCreateEmployeeActive = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BiometricSource() {
    }

    public BiometricSource(SourceType sourceType, String name, String normalizedCode) {
        this(sourceType, name, normalizedCode, true);
    }

    public BiometricSource(SourceType sourceType, String name, String normalizedCode, boolean active) {
        this.id = UUID.randomUUID().toString();
        this.sourceType = sourceType;
        this.name = name.strip();
        this.normalizedCode = normalizedCode.strip();
        this.active = active;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void configureAutoEmployeeCreation(boolean enabled, String categoryId, String employmentType,
                                              String activeFromMode, boolean employeeActive) {
        this.autoCreateEmployees = enabled;
        this.autoCreateCategoryId = enabled && categoryId != null && !categoryId.isBlank()
                ? categoryId.strip() : null;
        this.autoCreateEmploymentType = employmentType == null || employmentType.isBlank()
                ? "FIXED" : employmentType.strip().toUpperCase();
        this.autoCreateActiveFromMode = activeFromMode == null || activeFromMode.isBlank()
                ? "FIRST_PUNCH" : activeFromMode.strip().toUpperCase();
        this.autoCreateEmployeeActive = employeeActive;
    }

    public void update(String name, SourceType sourceType, String normalizedCode, boolean active) {
        this.name = name.strip();
        this.sourceType = sourceType;
        this.normalizedCode = normalizedCode.strip();
        this.active = active;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public SourceType getSourceType() { return sourceType; }
    public String getName() { return name; }
    public String getNormalizedCode() { return normalizedCode; }
    public boolean isActive() { return active; }
    public boolean isAutoCreateEmployees() { return autoCreateEmployees; }
    public String getAutoCreateCategoryId() { return autoCreateCategoryId; }
    public String getAutoCreateEmploymentType() { return autoCreateEmploymentType; }
    public String getAutoCreateActiveFromMode() { return autoCreateActiveFromMode; }
    public boolean isAutoCreateEmployeeActive() { return autoCreateEmployeeActive; }
    public Instant getCreatedAt() { return createdAt; }
}
