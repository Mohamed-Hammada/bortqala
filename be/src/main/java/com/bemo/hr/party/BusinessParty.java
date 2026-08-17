package com.bemo.hr.party;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "business_parties")
@Getter
public class BusinessParty {
    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(name = "name_en", length = 160)
    private String nameEn;
    @Column(name = "party_type", nullable = false, length = 80)
    private String partyType;
    @Column(name = "contact_person", length = 160)
    private String contactPerson;
    @Column(length = 50)
    private String phone;
    @Column(length = 100)
    private String email;
    @Column(length = 500)
    private String address;
    @Column(length = 1000)
    private String notes;
    @Column(name = "managed_type", nullable = false, length = 30)
    private String managedType;
    @Column(name = "responsible_party_id", length = 36)
    private String responsiblePartyId;
    @Column(name = "relationship_start_date", length = 10)
    private String relationshipStartDate;
    @Column(name = "relationship_end_date", length = 10)
    private String relationshipEndDate;
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;
    @Column(name = "invoice_policy", nullable = false, length = 30)
    private String invoicePolicy;
    @Column(name = "payment_terms", nullable = false, length = 30)
    private String paymentTerms;
    @Column(name = "tax_id", length = 50)
    private String taxId;
    @Column(name = "bank_account", length = 100)
    private String bankAccount;
    @Column(name = "onboarding_status", nullable = false, length = 30)
    private String onboardingStatus;
    @Column(name = "supplier_category", length = 50)
    private String supplierCategory;
    @Column(name = "risk_level", length = 20)
    private String riskLevel;
    @Column(name = "owner_user_id", length = 100)
    private String ownerUserId;
    @Column(name = "approval_instance_id", length = 36)
    private String approvalInstanceId;
    @Column(name = "bank_verified", nullable = false)
    private boolean bankVerified;
    @Column(name = "bank_verified_at")
    private Instant bankVerifiedAt;
    @Column(name = "bank_verified_by", length = 100)
    private String bankVerifiedBy;
    @Column(nullable = false)
    private boolean active;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessParty() {
    }

    public BusinessParty(String code, String name, String nameEn, String partyType,
                         String contactPerson, String phone, String email, String address,
                         String notes, boolean active,
                         String managedType, String responsiblePartyId,
                         String relationshipStartDate, String relationshipEndDate,
                         String currencyCode, String invoicePolicy, String paymentTerms,
                         String taxId, String bankAccount) {
        this.id = UUID.randomUUID().toString();
        update(code, name, nameEn, partyType, contactPerson, phone, email, address, notes, active,
                managedType, responsiblePartyId, relationshipStartDate, relationshipEndDate,
                currencyCode, invoicePolicy, paymentTerms, taxId, bankAccount);
        this.onboardingStatus = active ? "ACTIVE" : "REQUESTED";
        this.bankVerified = bankAccount != null && !bankAccount.isBlank();
    }

    public void update(String code, String name, String nameEn, String partyType,
                       String contactPerson, String phone, String email, String address,
                       String notes, boolean active,
                       String managedType, String responsiblePartyId,
                       String relationshipStartDate, String relationshipEndDate,
                       String currencyCode, String invoicePolicy, String paymentTerms,
                       String taxId, String bankAccount) {
        this.code = code.strip().toUpperCase(Locale.ROOT);
        this.name = name.strip();
        this.nameEn = nullable(nameEn);
        this.partyType = partyType.strip().toUpperCase(Locale.ROOT).replace(' ', '_');
        this.contactPerson = nullable(contactPerson);
        this.phone = nullable(phone);
        this.email = nullable(email);
        this.address = nullable(address);
        this.notes = nullable(notes);
        this.active = "SUPPLIER".equals(this.partyType) && onboardingStatus != null
                ? "ACTIVE".equals(onboardingStatus) : active;
        this.managedType = managedType;
        this.responsiblePartyId = nullable(responsiblePartyId);
        this.relationshipStartDate = nullable(relationshipStartDate);
        this.relationshipEndDate = nullable(relationshipEndDate);
        this.currencyCode = currencyCode;
        this.invoicePolicy = invoicePolicy;
        this.paymentTerms = paymentTerms;
        this.taxId = nullable(taxId);
        this.bankAccount = nullable(bankAccount);
    }

    public void updateSupplierProfile(String supplierCategory, String riskLevel, String ownerUserId) {
        this.supplierCategory = nullable(supplierCategory);
        this.riskLevel = nullable(riskLevel);
        this.ownerUserId = nullable(ownerUserId);
    }

    public void beginSupplierRequest() {
        requireSupplier();
        this.onboardingStatus = "REQUESTED";
        this.active = false;
        this.approvalInstanceId = null;
    }

    public void submitForReview(String approvalInstanceId) {
        requireStatus("REQUESTED");
        this.onboardingStatus = "UNDER_REVIEW";
        this.approvalInstanceId = nullable(approvalInstanceId);
        this.active = false;
    }

    public void approveOnboarding() {
        requireStatus("UNDER_REVIEW");
        this.onboardingStatus = "APPROVED";
        this.active = false;
    }

    public void activateSupplier() {
        requireStatus("APPROVED");
        this.onboardingStatus = "ACTIVE";
        this.active = true;
    }

    public void suspendSupplier() {
        requireSupplier();
        this.onboardingStatus = "SUSPENDED";
        this.active = false;
    }

    public void blacklistSupplier() {
        requireSupplier();
        this.onboardingStatus = "BLACKLISTED";
        this.active = false;
    }

    public void closeSupplier() {
        requireSupplier();
        this.onboardingStatus = "CLOSED";
        this.active = false;
    }

    public void verifyBank(String account, String actor, Instant verifiedAt) {
        this.bankAccount = nullable(account);
        this.bankVerified = true;
        this.bankVerifiedBy = actor;
        this.bankVerifiedAt = verifiedAt;
    }

    public boolean isProcurementAllowed() {
        return active && "SUPPLIER".equals(partyType) && "ACTIVE".equals(onboardingStatus);
    }

    public boolean isPaymentAllowed() {
        return isProcurementAllowed() && bankVerified;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void deactivate() {
        this.active = false;
    }

    public void clearPhone() {
        this.phone = null;
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

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private void requireSupplier() {
        if (!"SUPPLIER".equals(partyType))
            throw new IllegalStateException("Supplier lifecycle applies only to suppliers.");
    }

    private void requireStatus(String expected) {
        requireSupplier();
        if (!expected.equals(onboardingStatus)) {
            throw new IllegalStateException("Supplier must be " + expected + " for this transition.");
        }
    }
}
