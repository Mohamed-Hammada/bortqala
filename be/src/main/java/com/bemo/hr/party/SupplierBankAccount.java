package com.bemo.hr.party;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "supplier_bank_accounts")
@Getter
public class SupplierBankAccount {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;
    @Column(name = "account_name", nullable = false, length = 160)
    private String accountName;
    @Column(name = "iban", nullable = false, length = 100)
    private String iban;
    @Column(name = "normalized_iban", nullable = false, length = 100)
    private String normalizedIban;
    @Column(name = "bank_name", nullable = false, length = 160)
    private String bankName;
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;
    @Column(name = "is_primary", nullable = false)
    private boolean primary;
    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus;
    @Column(name = "verified_by", length = 100)
    private String verifiedBy;
    @Column(name = "verified_at")
    private Instant verifiedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SupplierBankAccount() {
    }

    public SupplierBankAccount(String supplierId, String accountName, String iban, String bankName,
                               String currencyCode, boolean primary) {
        this.id = UUID.randomUUID().toString();
        this.supplierId = supplierId;
        this.accountName = accountName.strip();
        this.iban = iban.strip();
        this.normalizedIban = normalize(iban);
        this.bankName = bankName.strip();
        this.currencyCode = currencyCode.strip().toUpperCase(Locale.ROOT);
        this.primary = primary;
        this.verificationStatus = "PENDING";
    }

    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    public void verify(String actor) {
        this.verificationStatus = "VERIFIED";
        this.verifiedBy = actor;
        this.verifiedAt = Instant.now();
    }

    public boolean isVerified() {
        return "VERIFIED".equals(verificationStatus);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
