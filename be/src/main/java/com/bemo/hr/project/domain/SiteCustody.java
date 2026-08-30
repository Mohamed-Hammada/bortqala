package com.bemo.hr.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "project_site_custodies")
public class SiteCustody {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "app_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "custody_code", length = 64, nullable = false)
    private String custodyCode;

    @Column(name = "custodian_employee_id", length = 36)
    private String custodianEmployeeId;

    @Column(name = "custodian_name", nullable = false)
    private String custodianName;

    @Column(name = "custody_type", length = 32, nullable = false)
    private String custodyType; // CASH, MATERIAL, EQUIPMENT

    @Column(name = "initial_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal initialAmount;

    @Column(name = "remaining_balance", precision = 14, scale = 2, nullable = false)
    private BigDecimal remainingBalance;

    @Column(name = "status", length = 32, nullable = false)
    private String status; // ACTIVE, SETTLED, CLOSED

    @Column(name = "issued_at", nullable = false)
    private long issuedAt;

    @Column(name = "settled_at")
    private Long settledAt;

    @Column(name = "notes")
    private String notes;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public SiteCustody() {}

    public SiteCustody(
            String id,
            String tenantId,
            String projectId,
            String custodyCode,
            String custodianEmployeeId,
            String custodianName,
            String custodyType,
            BigDecimal initialAmount,
            long issuedAt,
            String notes) {
        this.id = id;
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.custodyCode = custodyCode;
        this.custodianEmployeeId = custodianEmployeeId;
        this.custodianName = custodianName;
        this.custodyType = custodyType;
        this.initialAmount = initialAmount;
        this.remainingBalance = initialAmount;
        this.status = "ACTIVE";
        this.issuedAt = issuedAt;
        this.notes = notes;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getCustodyCode() {
        return custodyCode;
    }

    public String getCustodianEmployeeId() {
        return custodianEmployeeId;
    }

    public String getCustodianName() {
        return custodianName;
    }

    public String getCustodyType() {
        return custodyType;
    }

    public BigDecimal getInitialAmount() {
        return initialAmount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void deductBalance(BigDecimal amount) {
        this.remainingBalance = this.remainingBalance.subtract(amount);
        this.updatedAt = System.currentTimeMillis();
    }

    public String getStatus() {
        return status;
    }

    public void settle() {
        this.status = "SETTLED";
        this.settledAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public Long getSettledAt() {
        return settledAt;
    }

    public String getNotes() {
        return notes;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
