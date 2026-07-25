package com.bemo.license.domain;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="license_activations") @Getter
public class LicenseActivation {
    @Id private String id;
    @Column(name="license_id", nullable=false) private String licenseId;
    @Column(name="installation_id", nullable=false, length=80) private String installationId;
    @Column(name="device_fingerprint_hash", nullable=false, length=64) private String deviceFingerprintHash;
    @Column(name="device_public_key", nullable=false, length=500) private String devicePublicKey;
    @Column(name="expires_at") private Instant expiresAt;
    @Column(nullable=false) private boolean active;
    @Column(name="activated_at", nullable=false) private Instant activatedAt;
    @Column(name="last_validated_at") private Instant lastValidatedAt;
    @Column(name="deactivated_at") private Instant deactivatedAt;

    protected LicenseActivation() { }
    public LicenseActivation(String licenseId, String installationId, String fingerprint, String publicKey, Instant activatedAt, Instant expiresAt) {
        this.id=UUID.randomUUID().toString();this.licenseId=licenseId;this.installationId=installationId;this.deviceFingerprintHash=fingerprint;
        this.devicePublicKey=publicKey;this.activatedAt=activatedAt;this.expiresAt=expiresAt;this.active=true;this.lastValidatedAt=activatedAt;
    }
    public void validated(Instant at){this.lastValidatedAt=at;}
    public void deactivate(Instant at){this.active=false;this.deactivatedAt=at;}
}
