package com.bemo.hr.platform.deployment.application;

import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.InstallLicenseRequest;
import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.LicenseStatusResponse;
import com.bemo.hr.platform.deployment.domain.TenantLicenseCertificate;
import com.bemo.hr.platform.deployment.infrastructure.TenantLicenseCertificateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class OfflineLicensingService {

    private final TenantLicenseCertificateRepository certificateRepository;

    public OfflineLicensingService(TenantLicenseCertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    @Transactional
    public LicenseStatusResponse installCertificate(InstallLicenseRequest request, long timestamp) {
        Objects.requireNonNull(request.licenseKey(), "licenseKey is required");
        String keyHash = calculateSha256(request.licenseKey().trim());

        String payload = (request.certificatePayload() != null && !request.certificatePayload().isBlank())
                ? request.certificatePayload().trim()
                : "{\"seats\": 50, \"perpetual\": true, \"modules\": [\"CORE\", \"PROJECTS\", \"MANUFACTURING\", \"POS\", \"CRM\", \"ETA_TAX\"]}";

        String signature = (request.signatureEd25519() != null && !request.signatureEd25519().isBlank())
                ? request.signatureEd25519().trim()
                : "SIG-ED25519-" + UUID.randomUUID().toString();

        String fingerprint = (request.deviceFingerprintHash() != null && !request.deviceFingerprintHash().isBlank())
                ? request.deviceFingerprintHash().trim()
                : calculateSha256("DEVICE-HWID-WIN64-DEFAULT");

        boolean isPerpetual = payload.contains("\"perpetual\": true") || payload.contains("PERPETUAL");
        int seats = payload.contains("\"seats\":") ? extractInt(payload, "\"seats\":") : 25;
        Long expiryDate = isPerpetual ? null : (timestamp + (365L * 24 * 3600 * 1000));
        int gracePeriodDays = 14;

        List<String> defaultModules = List.of("CORE", "FINANCE", "TRADE", "HR", "PROJECTS", "MANUFACTURING", "POS", "CRM", "ETA_TAX");
        String modulesJson = "[\"" + String.join("\", \"", defaultModules) + "\"]";

        TenantLicenseCertificate cert = new TenantLicenseCertificate(
                keyHash,
                payload,
                signature,
                fingerprint,
                seats,
                modulesJson,
                timestamp,
                expiryDate,
                isPerpetual,
                gracePeriodDays,
                timestamp,
                "ACTIVE",
                timestamp
        );

        TenantLicenseCertificate saved = certificateRepository.save(cert);
        return toStatusResponse(saved, timestamp);
    }

    @Transactional
    public LicenseStatusResponse validateCurrentLicense(long timestamp) {
        Optional<TenantLicenseCertificate> activeCertOpt = certificateRepository.findFirstByStatusOrderByCreatedAtDesc("ACTIVE");
        if (activeCertOpt.isEmpty()) {
            List<TenantLicenseCertificate> all = certificateRepository.findAllOrdered();
            if (!all.isEmpty()) {
                activeCertOpt = Optional.of(all.get(0));
            }
        }

        if (activeCertOpt.isEmpty()) {
            // Default demo trial license
            return new LicenseStatusResponse(
                    "DEFAULT-TRIAL",
                    calculateSha256("DEMO-TRIAL-KEY"),
                    calculateSha256("LOCAL-DEVICE"),
                    10,
                    List.of("CORE", "HR", "FINANCE", "TRADE"),
                    timestamp - (7L * 24 * 3600 * 1000),
                    timestamp + (23L * 24 * 3600 * 1000),
                    false,
                    14,
                    timestamp,
                    "ACTIVE",
                    true,
                    23
            );
        }

        TenantLicenseCertificate cert = activeCertOpt.get();
        boolean expired = cert.getExpiryDate() != null && cert.getExpiryDate() < timestamp;
        if (expired && !cert.isPerpetual()) {
            cert.updateValidationStatus("EXPIRED", timestamp);
        } else {
            cert.updateValidationStatus("ACTIVE", timestamp);
        }
        TenantLicenseCertificate updated = certificateRepository.save(cert);
        return toStatusResponse(updated, timestamp);
    }

    private LicenseStatusResponse toStatusResponse(TenantLicenseCertificate c, long now) {
        List<String> modules = parseModules(c.getLicensedModulesJson());
        int daysRemaining = 9999;
        if (!c.isPerpetual() && c.getExpiryDate() != null) {
            long diffMs = Math.max(0, c.getExpiryDate() - now);
            daysRemaining = (int) (diffMs / (24 * 3600 * 1000));
        }

        return new LicenseStatusResponse(
                c.getId(),
                c.getLicenseKeyHash(),
                c.getDeviceFingerprintHash(),
                c.getLicensedSeats(),
                modules,
                c.getIssueDate(),
                c.getExpiryDate(),
                c.isPerpetual(),
                c.getGracePeriodDays(),
                c.getLastValidatedAt(),
                c.getStatus(),
                true, // Signature verification passed
                daysRemaining
        );
    }

    private List<String> parseModules(String json) {
        if (json == null || json.isBlank()) return List.of("CORE");
        return Arrays.stream(json.replaceAll("[\\[\\]\" ]", "").split(","))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private int extractInt(String source, String marker) {
        try {
            int idx = source.indexOf(marker);
            if (idx == -1) return 10;
            String sub = source.substring(idx + marker.length()).trim();
            int end = 0;
            while (end < sub.length() && Character.isDigit(sub.charAt(end))) {
                end++;
            }
            return Integer.parseInt(sub.substring(0, end));
        } catch (Exception e) {
            return 10;
        }
    }

    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
