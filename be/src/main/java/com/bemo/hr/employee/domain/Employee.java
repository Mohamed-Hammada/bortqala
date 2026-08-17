package com.bemo.hr.employee.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "device_user_id", unique = true, length = 100)
    private String deviceUserId;

    @Column(name = "category_id", nullable = false)
    private String categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    @Column(name = "base_salary", nullable = false)
    private java.math.BigDecimal baseSalary;

    @Column(name = "active_from", nullable = false)
    private LocalDate activeFrom;

    @Column(name = "active_to")
    private LocalDate activeTo;

    @Column(nullable = false)
    private boolean active;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Employee() {
    }

    public Employee(String employeeCode, String fullName, String deviceUserId, String categoryId,
                    EmploymentType employmentType, LocalDate activeFrom, LocalDate activeTo, boolean active) {
        this(employeeCode, fullName, deviceUserId, categoryId, employmentType, java.math.BigDecimal.ZERO, activeFrom, activeTo, active);
    }

    public Employee(String employeeCode, String fullName, String deviceUserId, String categoryId,
                    EmploymentType employmentType, java.math.BigDecimal baseSalary, LocalDate activeFrom, LocalDate activeTo, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(employeeCode, fullName, deviceUserId, categoryId, employmentType, baseSalary, activeFrom, activeTo, active);
    }

    public void update(String employeeCode, String fullName, String deviceUserId, String categoryId,
                       EmploymentType employmentType, java.math.BigDecimal baseSalary, LocalDate activeFrom, LocalDate activeTo, boolean active) {
        this.employeeCode = employeeCode.strip().toUpperCase(Locale.ROOT);
        this.fullName = fullName.strip();
        this.deviceUserId = deviceUserId == null || deviceUserId.isBlank() ? null : deviceUserId.strip();
        this.categoryId = categoryId;
        this.employmentType = employmentType;
        this.baseSalary = baseSalary == null ? java.math.BigDecimal.ZERO : baseSalary;
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
        this.active = active;
    }

    public boolean activeOn(LocalDate date) {
        return active && !date.isBefore(activeFrom) && (activeTo == null || !date.isAfter(activeTo));
    }

    public void applyEmployeeCode(String newCode, Instant updatedAt) {
        this.employeeCode = newCode.strip().toUpperCase(Locale.ROOT);
        this.updatedAt = updatedAt;
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

    public String getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public java.math.BigDecimal getBaseSalary() {
        return baseSalary == null ? java.math.BigDecimal.ZERO : baseSalary;
    }

    public LocalDate getActiveFrom() {
        return activeFrom;
    }

    public LocalDate getActiveTo() {
        return activeTo;
    }

    public boolean isActive() {
        return active;
    }

    public long getVersion() {
        return version;
    }
}
