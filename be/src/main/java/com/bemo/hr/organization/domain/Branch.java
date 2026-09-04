package com.bemo.hr.organization.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "branches")
public class Branch {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "company_id", nullable = false, length = 36)
    private String companyId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "is_main_branch", nullable = false)
    private boolean isMainBranch;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "commercial_registry", length = 50)
    private String commercialRegistry;

    @Column(name = "default_warehouse_id", length = 36)
    private String defaultWarehouseId;

    @Column(name = "default_cashbox_id", length = 36)
    private String defaultCashboxId;

    @Column(name = "default_bank_account_id", length = 36)
    private String defaultBankAccountId;

    @Column(name = "default_pos_terminal_id", length = 36)
    private String defaultPosTerminalId;

    @Column(name = "document_code_prefix", length = 20)
    private String documentCodePrefix;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected Branch() {
    }

    public Branch(String companyId, String code, String name, String location, boolean active) {
        this(companyId, code, name, location, active, false, null, null, null, null, null, null, null, null, null);
    }

    public Branch(
            String companyId,
            String code,
            String name,
            String location,
            boolean active,
            boolean isMainBranch,
            String phone,
            String email,
            String taxNumber,
            String commercialRegistry,
            String defaultWarehouseId,
            String defaultCashboxId,
            String defaultBankAccountId,
            String defaultPosTerminalId,
            String documentCodePrefix
    ) {
        this.id = UUID.randomUUID().toString();
        update(companyId, code, name, location, active, isMainBranch, phone, email, taxNumber, commercialRegistry,
                defaultWarehouseId, defaultCashboxId, defaultBankAccountId, defaultPosTerminalId, documentCodePrefix);
    }

    public void update(String companyId, String code, String name, String location, boolean active) {
        update(companyId, code, name, location, active, this.isMainBranch, this.phone, this.email,
                this.taxNumber, this.commercialRegistry, this.defaultWarehouseId, this.defaultCashboxId,
                this.defaultBankAccountId, this.defaultPosTerminalId, this.documentCodePrefix);
    }

    public void update(
            String companyId,
            String code,
            String name,
            String location,
            boolean active,
            boolean isMainBranch,
            String phone,
            String email,
            String taxNumber,
            String commercialRegistry,
            String defaultWarehouseId,
            String defaultCashboxId,
            String defaultBankAccountId,
            String defaultPosTerminalId,
            String documentCodePrefix
    ) {
        this.companyId = companyId != null ? companyId.strip() : "";
        this.code = code != null ? code.strip() : "";
        this.name = name != null ? name.strip() : "";
        this.location = location == null || location.isBlank() ? null : location.strip();
        this.active = active;
        this.isMainBranch = isMainBranch;
        this.phone = phone == null || phone.isBlank() ? null : phone.strip();
        this.email = email == null || email.isBlank() ? null : email.strip();
        this.taxNumber = taxNumber == null || taxNumber.isBlank() ? null : taxNumber.strip();
        this.commercialRegistry = commercialRegistry == null || commercialRegistry.isBlank() ? null : commercialRegistry.strip();
        this.defaultWarehouseId = defaultWarehouseId == null || defaultWarehouseId.isBlank() ? null : defaultWarehouseId.strip();
        this.defaultCashboxId = defaultCashboxId == null || defaultCashboxId.isBlank() ? null : defaultCashboxId.strip();
        this.defaultBankAccountId = defaultBankAccountId == null || defaultBankAccountId.isBlank() ? null : defaultBankAccountId.strip();
        this.defaultPosTerminalId = defaultPosTerminalId == null || defaultPosTerminalId.isBlank() ? null : defaultPosTerminalId.strip();
        this.documentCodePrefix = documentCodePrefix == null || documentCodePrefix.isBlank() ? null : documentCodePrefix.strip().toUpperCase();
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

    public String getCompanyId() {
        return companyId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isMainBranch() {
        return isMainBranch;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public String getCommercialRegistry() {
        return commercialRegistry;
    }

    public String getDefaultWarehouseId() {
        return defaultWarehouseId;
    }

    public String getDefaultCashboxId() {
        return defaultCashboxId;
    }

    public String getDefaultBankAccountId() {
        return defaultBankAccountId;
    }

    public String getDefaultPosTerminalId() {
        return defaultPosTerminalId;
    }

    public String getDocumentCodePrefix() {
        return documentCodePrefix;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
