package com.bemo.hr.shared.security.devicesigning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class DeviceSigningApi {

    private DeviceSigningApi() {
    }

    public record EnrollDeviceRequest(
            @NotBlank(message = "deviceIdentifier is required")
            String deviceIdentifier,
            @NotBlank(message = "deviceName is required")
            String deviceName,
            @NotBlank(message = "publicKey is required")
            String publicKey,
            @NotBlank(message = "algorithm is required")
            String algorithm
    ) {}

    public record RevokeDeviceRequest(
            String reason
    ) {}

    public record DeviceResponse(
            String id,
            String deviceIdentifier,
            String deviceName,
            String algorithm,
            String status,
            String revokedReason,
            Instant enrolledAt,
            Instant revokedAt,
            Instant lastUsedAt
    ) {
        public static DeviceResponse from(UserDevice device) {
            return new DeviceResponse(
                    device.getId(),
                    device.getDeviceIdentifier(),
                    device.getDeviceName(),
                    device.getAlgorithm(),
                    device.getStatus().name(),
                    device.getRevokedReason(),
                    device.getEnrolledAt(),
                    device.getRevokedAt(),
                    device.getLastUsedAt()
            );
        }
    }

    public record CreateChallengeRequest(
            @NotBlank(message = "deviceId is required")
            String deviceId,
            @NotBlank(message = "operationType is required")
            String operationType,
            @NotNull(message = "payload is required")
            String payload
    ) {}

    public record ChallengeResponse(
            String challengeId,
            String deviceId,
            String nonce,
            String operationType,
            String payloadHash,
            Instant expiresAt
    ) {}

    public record VerifySignatureRequest(
            @NotBlank(message = "challengeId is required")
            String challengeId,
            @NotBlank(message = "signature is required")
            String signature,
            @NotNull(message = "payload is required")
            String payload
    ) {}

    public record VerificationResponse(
            boolean verified,
            String challengeId,
            String deviceId,
            String operationType,
            Instant verifiedAt
    ) {}

    public record DeviceListResponse(
            List<DeviceResponse> devices
    ) {}
}
