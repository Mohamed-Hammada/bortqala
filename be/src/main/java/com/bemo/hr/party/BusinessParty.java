package com.bemo.hr.party;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "business_parties")
@Getter
public class BusinessParty {
    @Id private String id;
    @TenantId @Column(name = "app_id", nullable = false) private String appId;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(name = "name_en", length = 160) private String nameEn;
    @Column(name = "party_type", nullable = false, length = 80) private String partyType;
    @Column(name = "contact_person", length = 160) private String contactPerson;
    @Column(length = 50) private String phone;
    @Column(length = 100) private String email;
    @Column(length = 500) private String address;
    @Column(length = 1000) private String notes;
    @Column(name = "managed_type", nullable = false, length = 30) private String managedType;
    @Column(name = "responsible_party_id", length = 36) private String responsiblePartyId;
    @Column(name = "relationship_start_date", length = 10) private String relationshipStartDate;
    @Column(name = "relationship_end_date", length = 10) private String relationshipEndDate;
    @Column(name = "currency_code", nullable = false, length = 10) private String currencyCode;
    @Column(name = "invoice_policy", nullable = false, length = 30) private String invoicePolicy;
    @Column(name = "payment_terms", nullable = false, length = 30) private String paymentTerms;
    @Column(name = "tax_id", length = 50) private String taxId;
    @Column(name = "bank_account", length = 100) private String bankAccount;
    @Column(nullable = false) private boolean active;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected BusinessParty() { }

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
        this.active = active;
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

    public void deactivate() { this.active = false; }
    public void clearPhone() { this.phone = null; }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
