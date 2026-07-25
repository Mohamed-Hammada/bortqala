package com.bemo.license.api;

import com.bemo.license.domain.LicenseType;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class LicenseApi {
    private LicenseApi() { }
    public record CreateLicenseRequest(@NotBlank @Size(max=160) String customerReference, @NotNull LicenseType licenseType,
                                       @Min(1) @Max(100) Integer durationYears, Instant validUntil,
                                       @Min(1) @Max(100) int maxActivations) { }
    public record CreatedLicense(String id, String licenseKey, String customerReference, LicenseType licenseType,
                                 Integer durationYears, Instant validUntil, int maxActivations) { }
    public record ActivateRequest(@NotBlank String licenseKey, @NotBlank @Size(max=80) String installationId,
                                  @Pattern(regexp="[a-fA-F0-9]{64}") String deviceFingerprintHash,
                                  @NotBlank @Size(max=500) String devicePublicKey, @NotNull Instant timestamp,
                                  @NotBlank String signature) { }
    public record ProofRequest(@NotBlank String activationId, @NotBlank @Size(max=100) String nonce,
                               @NotNull Instant timestamp, @NotBlank String signature) { }
    public record LicenseCertificate(String activationId, String licenseId, String customerReference,
                                     String installationId, String deviceFingerprintHash, Instant issuedAt,
                                     Instant expiresAt, boolean perpetual, String signature) { }
    public record DeactivationResult(String activationId, boolean deactivated, Instant deactivatedAt) { }
}
