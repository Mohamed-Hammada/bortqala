package com.bemo.hr.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "bank_name", nullable = false, length = 255)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;

    @Column(length = 100)
    private String iban;

    @Column(name = "swift_code", length = 50)
    private String swiftCode;

    @Column(name = "account_id", length = 36)
    private String accountId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected BankAccount() {}

    public BankAccount(String bankName, String accountNumber, String iban, String swiftCode, String accountId, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(bankName, accountNumber, iban, swiftCode, accountId, active);
    }

    public void update(String bankName, String accountNumber, String iban, String swiftCode, String accountId, boolean active) {
        this.bankName = bankName.strip();
        this.accountNumber = accountNumber.strip();
        this.iban = iban == null ? null : iban.strip();
        this.swiftCode = swiftCode == null ? null : swiftCode.strip();
        this.accountId = accountId == null || accountId.isBlank() ? null : accountId.strip();
        this.active = active;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); updatedAt = createdAt; }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getIban() { return iban; }
    public String getSwiftCode() { return swiftCode; }
    public String getAccountId() { return accountId; }
    public boolean isActive() { return active; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
