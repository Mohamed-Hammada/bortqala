package com.bemo.hr.payroll.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "payroll_components")
public class PayrollComponent {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "code", nullable = false, length = 50)
    private String code;
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;
    @Column(name = "calculation_formula", length = 255)
    private String calculationFormula;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected PayrollComponent() {
    }

    public PayrollComponent(String code, String name, Type type, String calculationFormula) {
        this.id = UUID.randomUUID().toString();
        this.code = code;
        this.name = name;
        this.type = type;
        this.calculationFormula = calculationFormula;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        createdAt = System.currentTimeMillis();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
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

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String getCalculationFormula() {
        return calculationFormula;
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

    public long getVersion() {
        return version;
    }

    public enum Type {
        EARNING, DEDUCTION
    }
}
