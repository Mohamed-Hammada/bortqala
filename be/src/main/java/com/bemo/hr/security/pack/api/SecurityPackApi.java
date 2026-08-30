package com.bemo.hr.security.pack.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SecurityPackApi {
    private SecurityPackApi() {
    }

    public record TotpEnrollResponse(String secret, String otpauthUri, List<String> backupCodes) {
    }

    public record TotpActivateRequest(@NotBlank @Size(min = 6, max = 8) String code) {
    }

    public record TotpVerifyRequest(@NotBlank String challengeToken,
                                    @NotBlank @Size(min = 6, max = 16) String code) {
    }

    public record TotpDisableRequest(@NotBlank String password) {
    }

    public record RegenerateBackupCodesRequest(@NotBlank String codeOrPassword) {
    }

    public record TotpStatusResponse(boolean enabled, Instant enabledAt, int remainingBackupCodes) {
    }

    public record SecurityPolicyResponse(
            int minPasswordLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigits,
            boolean requireSpecialChars,
            int passwordHistoryCount,
            int maxPasswordAgeDays,
            int sessionTimeoutMinutes,
            boolean superAdminIpBypass
    ) {
    }

    public record SecurityPolicyUpdateRequest(
            @Min(6) @Max(128) int minPasswordLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigits,
            boolean requireSpecialChars,
            @Min(0) @Max(24) int passwordHistoryCount,
            @Min(0) @Max(365) int maxPasswordAgeDays,
            @Min(5) @Max(1440) int sessionTimeoutMinutes,
            boolean superAdminIpBypass
    ) {
    }

    public record TrustedDeviceResponse(
            String id,
            String deviceId,
            String deviceLabel,
            String userAgent,
            String ipAddress,
            Instant lastSeenAt,
            boolean revoked,
            Instant revokedAt
    ) {
    }

    public record RoleIpRuleResponse(
            String id,
            String roleCode,
            String cidrBlock,
            String description,
            Instant createdAt
    ) {
    }

    public record RoleIpRuleCreateRequest(
            @NotBlank String roleCode,
            @NotBlank String cidrBlock,
            String description
    ) {
    }
}
