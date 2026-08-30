package com.bemo.hr.shared.security.devicesigning;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class DeviceSigningService {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private final UserDeviceRepository deviceRepository;
    private final DeviceSigningChallengeRepository challengeRepository;
    private final DeviceSignatureLogRepository signatureLogRepository;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeviceSigningService(UserDeviceRepository deviceRepository,
                                DeviceSigningChallengeRepository challengeRepository,
                                DeviceSignatureLogRepository signatureLogRepository,
                                AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.challengeRepository = challengeRepository;
        this.signatureLogRepository = signatureLogRepository;
        this.auditService = auditService;
    }

    @Transactional
    public UserDevice enrollDevice(String userId, String username, String deviceIdentifier,
                                   String deviceName, String publicKeyBase64, String algorithm) {
        if (deviceRepository.existsByUserIdAndDeviceIdentifier(userId, deviceIdentifier)) {
            throw new BusinessRuleException("Device identifier already registered", "DEVICE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        // Validate public key format
        validatePublicKey(publicKeyBase64, algorithm);

        UserDevice device = new UserDevice(userId, deviceIdentifier, deviceName, publicKeyBase64, algorithm);
        UserDevice saved = deviceRepository.save(device);

        auditService.record("DEVICE_REGISTERED", "UserDevice", saved.getId(), username,
                String.format("{\"deviceIdentifier\":\"%s\",\"deviceName\":\"%s\",\"algorithm\":\"%s\"}",
                        deviceIdentifier, deviceName, algorithm), "0.0.0.0");

        return saved;
    }

    @Transactional
    public void revokeDevice(String userId, String username, String deviceId, String reason) {
        UserDevice device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new BusinessRuleException("Device not found", "DEVICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!device.isActive()) {
            throw new BusinessRuleException("Device is already revoked", "DEVICE_REVOKED", HttpStatus.CONFLICT);
        }

        device.revoke(reason);
        deviceRepository.save(device);

        auditService.record("DEVICE_REVOKED", "UserDevice", device.getId(), username,
                String.format("{\"reason\":\"%s\"}", reason), "0.0.0.0");
    }

    @Transactional(readOnly = true)
    public List<UserDevice> listUserDevices(String userId) {
        return deviceRepository.findByUserIdOrderByEnrolledAtDesc(userId);
    }

    @Transactional
    public DeviceSigningChallenge createChallenge(String userId, String deviceId, String operationType, String payload) {
        UserDevice device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new BusinessRuleException("Device not found", "DEVICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!device.isActive()) {
            throw new BusinessRuleException("Device is not active", "DEVICE_NOT_ACTIVE", HttpStatus.FORBIDDEN);
        }

        String nonce = generateSecureNonce();
        String payloadHash = calculateSha256(payload);
        Instant expiresAt = Instant.now().plus(CHALLENGE_TTL);

        DeviceSigningChallenge challenge = new DeviceSigningChallenge(
                userId, deviceId, nonce, operationType, payloadHash, expiresAt
        );

        return challengeRepository.save(challenge);
    }

    @Transactional
    public boolean verifySignature(String userId, String username, String challengeId, String signatureBase64, String payload) {
        DeviceSigningChallenge challenge = challengeRepository.findByIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new BusinessRuleException("Challenge not found", "SIGNING_CHALLENGE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!challenge.isPending()) {
            throw new BusinessRuleException("Challenge already used or expired", "SIGNING_CHALLENGE_ALREADY_USED", HttpStatus.CONFLICT);
        }

        if (challenge.isExpired()) {
            challenge.markExpired();
            challengeRepository.save(challenge);
            throw new BusinessRuleException("Challenge expired", "SIGNING_CHALLENGE_EXPIRED", HttpStatus.GONE);
        }

        UserDevice device = deviceRepository.findByIdAndUserId(challenge.getDeviceId(), userId)
                .orElseThrow(() -> new BusinessRuleException("Device not found", "DEVICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!device.isActive()) {
            logSignatureResult(userId, device.getId(), challengeId, challenge.getOperationType(), signatureBase64, "REVOKED");
            throw new BusinessRuleException("Device is revoked", "DEVICE_REVOKED", HttpStatus.FORBIDDEN);
        }

        // Verify payload integrity
        String currentPayloadHash = calculateSha256(payload);
        if (!currentPayloadHash.equalsIgnoreCase(challenge.getPayloadHash())) {
            logSignatureResult(userId, device.getId(), challengeId, challenge.getOperationType(), signatureBase64, "TAMPERED");
            throw new BusinessRuleException("Payload tampered", "SIGNING_PAYLOAD_TAMPERED", HttpStatus.BAD_REQUEST);
        }

        // Verify cryptographic signature: data signed = nonce + ":" + payloadHash
        String signedData = challenge.getNonce() + ":" + challenge.getPayloadHash();
        boolean valid = verifyCryptoSignature(device.getPublicKey(), device.getAlgorithm(), signedData, signatureBase64);

        if (!valid) {
            logSignatureResult(userId, device.getId(), challengeId, challenge.getOperationType(), signatureBase64, "REJECTED");
            auditService.record("DEVICE_SIGNATURE_FAILED", "DeviceSigningChallenge", challengeId, username,
                    String.format("{\"deviceId\":\"%s\",\"operation\":\"%s\"}", device.getId(), challenge.getOperationType()), "0.0.0.0");
            throw new BusinessRuleException("Invalid device signature", "INVALID_DEVICE_SIGNATURE", HttpStatus.UNAUTHORIZED);
        }

        // Success: mark used, record usage on device, log signature
        challenge.markUsed();
        challengeRepository.save(challenge);

        device.recordUsage();
        deviceRepository.save(device);

        logSignatureResult(userId, device.getId(), challengeId, challenge.getOperationType(), signatureBase64, "VERIFIED");
        auditService.record("DEVICE_SIGNATURE_VERIFIED", "DeviceSigningChallenge", challengeId, username,
                String.format("{\"deviceId\":\"%s\",\"operation\":\"%s\"}", device.getId(), challenge.getOperationType()), "0.0.0.0");

        return true;
    }

    private void logSignatureResult(String userId, String deviceId, String challengeId, String operationType, String signatureValue, String status) {
        DeviceSignatureLog logEntry = new DeviceSignatureLog(userId, deviceId, challengeId, operationType, signatureValue, status);
        signatureLogRepository.save(logEntry);
    }

    private String generateSecureNonce() {
        byte[] nonceBytes = new byte[32];
        secureRandom.nextBytes(nonceBytes);
        return HexFormat.of().formatHex(nonceBytes);
    }

    public String calculateSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private void validatePublicKey(String publicKeyBase64, String algorithm) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            String keyFactoryAlgorithm = resolveKeyFactoryAlgorithm(algorithm);
            KeyFactory keyFactory = KeyFactory.getInstance(keyFactoryAlgorithm);
            keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new BusinessRuleException("Invalid public key format", "DEVICE_PUBLIC_KEY_INVALID", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean verifyCryptoSignature(String publicKeyBase64, String algorithm, String data, String signatureBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            String keyFactoryAlgorithm = resolveKeyFactoryAlgorithm(algorithm);
            KeyFactory keyFactory = KeyFactory.getInstance(keyFactoryAlgorithm);
            PublicKey pubKey = keyFactory.generatePublic(spec);

            String sigAlgorithm = resolveSignatureAlgorithm(algorithm);
            Signature sig = Signature.getInstance(sigAlgorithm);
            sig.initVerify(pubKey);
            sig.update(dataBytes);
            return sig.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveKeyFactoryAlgorithm(String algorithm) {
        String normalized = algorithm.toUpperCase();
        if (normalized.contains("RSA")) {
            return "RSA";
        } else if (normalized.contains("ECDSA") || normalized.contains("EC")) {
            return "EC";
        } else if (normalized.contains("ED25519") || normalized.contains("EDDSA")) {
            return "Ed25519";
        }
        throw new BusinessRuleException("Unsupported signing algorithm: " + algorithm, "SIGNING_ALGORITHM_NOT_SUPPORTED", HttpStatus.BAD_REQUEST);
    }

    private String resolveSignatureAlgorithm(String algorithm) {
        String normalized = algorithm.toUpperCase();
        if (normalized.contains("RSA")) {
            return "SHA256withRSA";
        } else if (normalized.contains("ECDSA") || normalized.contains("EC")) {
            return "SHA256withECDSA";
        } else if (normalized.contains("ED25519") || normalized.contains("EDDSA")) {
            return "Ed25519";
        }
        throw new BusinessRuleException("Unsupported signing algorithm: " + algorithm, "SIGNING_ALGORITHM_NOT_SUPPORTED", HttpStatus.BAD_REQUEST);
    }
}
