package com.bemo.license;

import com.bemo.license.api.LicenseApi;
import com.bemo.license.application.LicenseService;
import com.bemo.license.domain.LicenseType;
import com.bemo.license.infrastructure.LicenseActivationRepository;
import com.bemo.license.infrastructure.LicenseKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:license;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "license.admin.key=test-admin-key", "license.signing.allow-ephemeral=true"
})
class LicenseApplicationTests {
    @Autowired LicenseService licenseService;
    @Autowired LicenseActivationRepository licenseActivationRepository;
    @Autowired LicenseKeyRepository licenseKeyRepository;

    @BeforeEach
    void cleanDatabase() {
        licenseActivationRepository.deleteAll();
        licenseKeyRepository.deleteAll();
    }

    @Test void contextLoads() { }

    @Test
    void desktopCanDeactivateAndReactivateTheSameInstallation() throws Exception {
        var created = licenseService.create("test-admin-key",
                new LicenseApi.CreateLicenseRequest("Desktop customer", LicenseType.PERPETUAL,
                        null, null, 1));
        KeyPair device = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String installationId = "desktop-installation-1";
        String fingerprint = "a".repeat(64);

        var first = licenseService.activate(activationRequest(created.licenseKey(), installationId,
                fingerprint, device));
        licenseService.deactivate(proofRequest(first.activationId(), device));
        var second = licenseService.activate(activationRequest(created.licenseKey(), installationId,
                fingerprint, device));

        assertThat(second.activationId()).isEqualTo(first.activationId());
        assertThat(licenseActivationRepository.countByLicenseIdAndActiveTrue(created.id())).isEqualTo(1);
    }

    private LicenseApi.ActivateRequest activationRequest(String key, String installationId,
                                                         String fingerprint, KeyPair device) throws Exception {
        Instant timestamp = Instant.now();
        String canonical = "activate|" + installationId + '|' + fingerprint + '|' + timestamp;
        return new LicenseApi.ActivateRequest(key, installationId, fingerprint,
                Base64.getEncoder().encodeToString(device.getPublic().getEncoded()), timestamp,
                sign(device, canonical));
    }

    private LicenseApi.ProofRequest proofRequest(String activationId, KeyPair device) throws Exception {
        Instant timestamp = Instant.now();
        String nonce = "reactivation-test-nonce";
        String canonical = "proof|" + activationId + '|' + nonce + '|' + timestamp;
        return new LicenseApi.ProofRequest(activationId, nonce, timestamp, sign(device, canonical));
    }

    private String sign(KeyPair pair, String value) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(pair.getPrivate());
        signature.update(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
