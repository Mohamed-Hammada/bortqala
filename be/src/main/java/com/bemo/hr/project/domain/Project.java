package com.bemo.hr.project.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "company_id", length = 36)
    private String companyId;

    @Column(name = "branch_id", length = 36)
    private String branchId;

    @Column(name = "owner_party_id", length = 36)
    private String ownerPartyId;

    @Column(name = "project_manager_id", length = 36)
    private String projectManagerId;

    @Column(name = "site_address", length = 500)
    private String siteAddress;

    @Column(name = "contract_number", length = 100)
    private String contractNumber;

    @Column(name = "contract_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal contractValue = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "EGP";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status = ProjectStatus.DRAFT;

    @Column(name = "budget_blocking", nullable = false)
    private boolean budgetBlocking = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Project() {
    }

    public Project(String code, String name, String nameEn, String description,
                   String companyId, String branchId, String ownerPartyId,
                   String projectManagerId, String siteAddress, String contractNumber,
                   BigDecimal contractValue, String currencyCode,
                   LocalDate startDate, LocalDate endDate, boolean budgetBlocking) {
        this.id = UUID.randomUUID().toString();
        this.code = code != null ? code.strip() : null;
        this.status = ProjectStatus.DRAFT;
        this.active = true;
        update(name, nameEn, description, companyId, branchId, ownerPartyId,
                projectManagerId, siteAddress, contractNumber, contractValue,
                currencyCode, startDate, endDate, budgetBlocking);
    }

    public void update(String name, String nameEn, String description,
                       String companyId, String branchId, String ownerPartyId,
                       String projectManagerId, String siteAddress, String contractNumber,
                       BigDecimal contractValue, String currencyCode,
                       LocalDate startDate, LocalDate endDate, boolean budgetBlocking) {
        if (this.status == ProjectStatus.CLOSED) {
            throw new IllegalStateException("PROJECT_CLOSED");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("PROJECT_INVALID_DATES");
        }
        this.name = name.strip();
        this.nameEn = nameEn != null && !nameEn.isBlank() ? nameEn.strip() : null;
        this.description = description != null && !description.isBlank() ? description.strip() : null;
        this.companyId = companyId != null && !companyId.isBlank() ? companyId.strip() : null;
        this.branchId = branchId != null && !branchId.isBlank() ? branchId.strip() : null;
        this.ownerPartyId = ownerPartyId != null && !ownerPartyId.isBlank() ? ownerPartyId.strip() : null;
        this.projectManagerId = projectManagerId != null && !projectManagerId.isBlank() ? projectManagerId.strip() : null;
        this.siteAddress = siteAddress != null && !siteAddress.isBlank() ? siteAddress.strip() : null;
        this.contractNumber = contractNumber != null && !contractNumber.isBlank() ? contractNumber.strip() : null;
        this.contractValue = contractValue != null && contractValue.signum() >= 0 ? contractValue : BigDecimal.ZERO;
        this.currencyCode = currencyCode != null && !currencyCode.isBlank() ? currencyCode.strip().toUpperCase() : "EGP";
        this.startDate = startDate;
        this.endDate = endDate;
        this.budgetBlocking = budgetBlocking;
    }

    public void activate() {
        if (this.status == ProjectStatus.CLOSED) {
            throw new IllegalStateException("PROJECT_CLOSED");
        }
        this.status = ProjectStatus.ACTIVE;
        this.active = true;
    }

    public void hold() {
        if (this.status == ProjectStatus.CLOSED) {
            throw new IllegalStateException("PROJECT_CLOSED");
        }
        this.status = ProjectStatus.ON_HOLD;
    }

    public void complete() {
        if (this.status == ProjectStatus.CLOSED) {
            throw new IllegalStateException("PROJECT_CLOSED");
        }
        this.status = ProjectStatus.COMPLETED;
    }

    public void close() {
        this.status = ProjectStatus.CLOSED;
        this.active = false;
    }

    public void reopen() {
        this.status = ProjectStatus.ACTIVE;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
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

    public String getName() {
        return name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDescription() {
        return description;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getOwnerPartyId() {
        return ownerPartyId;
    }

    public String getProjectManagerId() {
        return projectManagerId;
    }

    public String getSiteAddress() {
        return siteAddress;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public BigDecimal getContractValue() {
        return contractValue;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public boolean isBudgetBlocking() {
        return budgetBlocking;
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
}
