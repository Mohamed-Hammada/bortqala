package com.bemo.license.domain;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "license_keys") @Getter
public class LicenseKey {
    @Id private String id;
    @Column(name="key_hash", nullable=false, unique=true, length=64) private String keyHash;
    @Column(name="customer_reference", nullable=false, length=160) private String customerReference;
    @Enumerated(EnumType.STRING) @Column(name="license_type", nullable=false, length=20) private LicenseType licenseType;
    @Column(name="duration_years") private Integer durationYears;
    @Column(name="valid_until") private Instant validUntil;
    @Column(name="max_activations", nullable=false) private int maxActivations;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private LicenseStatus status;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;

    protected LicenseKey() { }
    public LicenseKey(String keyHash, String customerReference, LicenseType type, Integer years, Instant validUntil, int maxActivations) {
        this.id=UUID.randomUUID().toString(); this.keyHash=keyHash; this.customerReference=customerReference.strip();
        this.licenseType=type; this.durationYears=years; this.validUntil=validUntil; this.maxActivations=maxActivations; this.status=LicenseStatus.ACTIVE;
    }
    public Instant expiryFrom(Instant activationTime) {
        return switch (licenseType) { case PERPETUAL -> null; case FIXED_DATE -> validUntil;
            case TERM_YEARS -> activationTime.atZone(ZoneOffset.UTC).plusYears(durationYears).toInstant(); };
    }
    @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=Instant.now();}
}
