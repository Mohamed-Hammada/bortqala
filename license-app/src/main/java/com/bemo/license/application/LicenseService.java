package com.bemo.license.application;

import com.bemo.license.api.LicenseApi;
import com.bemo.license.domain.*;
import com.bemo.license.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.HexFormat;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class LicenseService {
    private final LicenseKeyRepository licenseKeyRepository;
    private final LicenseActivationRepository licenseActivationRepository;
    private final LicenseCryptoService licenseCryptoService;
    @Value("${license.admin.key}") private String adminKey;
    @Value("${license.proof.max-clock-skew:PT5M}") private Duration maxClockSkew;

    @Transactional
    public LicenseApi.CreatedLicense create(String suppliedAdminKey, LicenseApi.CreateLicenseRequest request) {
        requireAdmin(suppliedAdminKey); validateDefinition(request);
        String raw = "BEMO-" + randomHex(20).toUpperCase();
        var license = licenseKeyRepository.save(new LicenseKey(hash(normalize(raw)), request.customerReference(),
                request.licenseType(), request.durationYears(), request.validUntil(), request.maxActivations()));
        return new LicenseApi.CreatedLicense(license.getId(), raw, license.getCustomerReference(), license.getLicenseType(),
                license.getDurationYears(), license.getValidUntil(), license.getMaxActivations());
    }

    @Transactional
    public LicenseApi.LicenseCertificate activate(LicenseApi.ActivateRequest request) {
        requireFresh(request.timestamp());
        String canonical = "activate|" + request.installationId() + '|' + request.deviceFingerprintHash().toLowerCase() + '|' + request.timestamp();
        if (!licenseCryptoService.verifyDevice(request.devicePublicKey(), canonical, request.signature())) throw new LicenseException("DEVICE_PROOF_INVALID", "Device proof is invalid.");
        var license = licenseKeyRepository.findByKeyHash(hash(normalize(request.licenseKey())))
                .orElseThrow(() -> new LicenseException("LICENSE_NOT_FOUND", "License key is invalid."));
        requireUsable(license, Instant.now());
        var existing = licenseActivationRepository.findByLicenseIdAndInstallationIdAndActiveTrue(license.getId(), request.installationId());
        var activation = existing.orElseGet(() -> {
            if (licenseActivationRepository.countByLicenseIdAndActiveTrue(license.getId()) >= license.getMaxActivations())
                throw new LicenseException("ACTIVATION_LIMIT_REACHED", "License is already active on the allowed number of devices.");
            Instant now=Instant.now(); return licenseActivationRepository.save(new LicenseActivation(license.getId(), request.installationId(),
                    request.deviceFingerprintHash().toLowerCase(), request.devicePublicKey(), now, license.expiryFrom(now)));
        });
        if (!MessageDigest.isEqual(activation.getDeviceFingerprintHash().getBytes(StandardCharsets.UTF_8), request.deviceFingerprintHash().toLowerCase().getBytes(StandardCharsets.UTF_8)))
            throw new LicenseException("DEVICE_MISMATCH", "This installation id is bound to another device.");
        return certificate(license, activation);
    }

    @Transactional
    public LicenseApi.LicenseCertificate validate(LicenseApi.ProofRequest request) {
        var activation = requireProof(request);
        var license = licenseKeyRepository.findById(activation.getLicenseId()).orElseThrow();
        requireUsable(license, Instant.now());
        if (activation.getExpiresAt()!=null && !Instant.now().isBefore(activation.getExpiresAt())) throw new LicenseException("LICENSE_EXPIRED", "License has expired.");
        activation.validated(Instant.now()); return certificate(license, activation);
    }

    @Transactional
    public LicenseApi.DeactivationResult deactivate(LicenseApi.ProofRequest request) {
        var activation=requireProof(request); Instant now=Instant.now(); activation.deactivate(now);
        return new LicenseApi.DeactivationResult(activation.getId(), true, now);
    }

    private LicenseActivation requireProof(LicenseApi.ProofRequest request) {
        requireFresh(request.timestamp());
        var activation=licenseActivationRepository.findById(request.activationId()).filter(LicenseActivation::isActive)
                .orElseThrow(() -> new LicenseException("ACTIVATION_NOT_FOUND", "Active installation was not found."));
        String canonical="proof|"+request.activationId()+'|'+request.nonce()+'|'+request.timestamp();
        if(!licenseCryptoService.verifyDevice(activation.getDevicePublicKey(),canonical,request.signature())) throw new LicenseException("DEVICE_PROOF_INVALID","Device proof is invalid.");
        return activation;
    }
    private LicenseApi.LicenseCertificate certificate(LicenseKey license, LicenseActivation activation) {
        Instant issuedAt=Instant.now(); boolean perpetual=activation.getExpiresAt()==null;
        String canonical=activation.getId()+'|'+license.getId()+'|'+license.getCustomerReference()+'|'+activation.getInstallationId()+'|'+activation.getDeviceFingerprintHash()+'|'+issuedAt+'|'+activation.getExpiresAt()+'|'+perpetual;
        return new LicenseApi.LicenseCertificate(activation.getId(),license.getId(),license.getCustomerReference(),activation.getInstallationId(),
                activation.getDeviceFingerprintHash(),issuedAt,activation.getExpiresAt(),perpetual,licenseCryptoService.sign(canonical));
    }
    private void requireUsable(LicenseKey license, Instant now){if(license.getStatus()!=LicenseStatus.ACTIVE)throw new LicenseException("LICENSE_DISABLED","License is not active.");if(license.getLicenseType()==LicenseType.FIXED_DATE&&!now.isBefore(license.getValidUntil()))throw new LicenseException("LICENSE_EXPIRED","License has expired.");}
    private void requireFresh(Instant timestamp){if(Duration.between(timestamp,Instant.now()).abs().compareTo(maxClockSkew)>0)throw new LicenseException("STALE_PROOF","Device proof timestamp is outside the allowed window.");}
    private void validateDefinition(LicenseApi.CreateLicenseRequest request){if(request.licenseType()==LicenseType.TERM_YEARS&&request.durationYears()==null)throw new LicenseException("DURATION_REQUIRED","Term licenses require durationYears.");if(request.licenseType()==LicenseType.FIXED_DATE&&(request.validUntil()==null||!request.validUntil().isAfter(Instant.now())))throw new LicenseException("EXPIRY_REQUIRED","Fixed-date licenses require a future validUntil.");}
    private void requireAdmin(String supplied){if(supplied==null||!MessageDigest.isEqual(adminKey.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new LicenseException("ADMIN_UNAUTHORIZED","Admin key is invalid.");}
    private String normalize(String value){return value.replace("-","").strip().toUpperCase();}
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private String randomHex(int bytes){byte[] value=new byte[bytes];new SecureRandom().nextBytes(value);return HexFormat.of().formatHex(value);}
}
