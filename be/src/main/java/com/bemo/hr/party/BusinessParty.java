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
    @Column(name = "party_type", nullable = false, length = 80) private String partyType;
    @Column(name = "contact_person", length = 160) private String contactPerson;
    @Column(length = 50) private String phone;
    @Column(length = 1000) private String notes;
    @Column(nullable = false) private boolean active;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected BusinessParty() { }

    public BusinessParty(String code, String name, String partyType, String contactPerson,
                         String phone, String notes, boolean active) {
        this.id = UUID.randomUUID().toString();
        update(code, name, partyType, contactPerson, phone, notes, active);
    }

    public void update(String code, String name, String partyType, String contactPerson,
                       String phone, String notes, boolean active) {
        this.code = code.strip().toUpperCase(Locale.ROOT);
        this.name = name.strip();
        this.partyType = partyType.strip().toUpperCase(Locale.ROOT).replace(' ', '_');
        this.contactPerson = nullable(contactPerson);
        this.phone = nullable(phone);
        this.notes = nullable(notes);
        this.active = active;
    }

    public void deactivate() { this.active = false; }

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
