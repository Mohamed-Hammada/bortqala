package com.bemo.hr.shared.security.devicesigning;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceSigningServiceTests {

    @Mock
    private UserDeviceRepository deviceRepository;

    @Mock
    private DeviceSigningChallengeRepository challengeRepository;

    @Mock
    private DeviceSignatureLogRepository signatureLogRepository;

    @Mock
    private AuditService auditService;

    private DeviceSigningService service;

    private KeyPair ecKeyPair;
    private KeyPair rsaKeyPair;
    private String ecPublicKeyBase64;
    private String rsaPublicKeyBase64;

    @BeforeEach
    void setUp() throws Exception {
        service = new DeviceSigningService(deviceRepository, challengeRepository, signatureLogRepository, auditService);

        // Generate EC P-256 keys
        KeyPairGenerator ecGen = KeyPairGenerator.getInstance("EC");
        ecGen.initialize(256);
        ecKeyPair = ecGen.generateKeyPair();
        ecPublicKeyBase64 = Base64.getEncoder().encodeToString(ecKeyPair.getPublic().getEncoded());

        // Generate RSA keys
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(2048);
        rsaKeyPair = rsaGen.generateKeyPair();
        rsaPublicKeyBase64 = Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
    }

    @Test
    void enrollDevice_success_and_audited() {
        when(deviceRepository.existsByUserIdAndDeviceIdentifier("u1", "dev-01")).thenReturn(false);
        when(deviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDevice device = service.enrollDevice("u1", "admin", "dev-01", "MacBook Pro", ecPublicKeyBase64, "ECDSA-P256");

        assertNotNull(device);
        assertEquals("dev-01", device.getDeviceIdentifier());
        assertEquals("MacBook Pro", device.getDeviceName());
        assertEquals(DeviceStatus.ACTIVE, device.getStatus());
        verify(auditService).record(eq("DEVICE_REGISTERED"), eq("UserDevice"), anyString(), eq("admin"), anyString(), anyString());
    }

    @Test
    void enrollDevice_duplicate_throwsException() {
        when(deviceRepository.existsByUserIdAndDeviceIdentifier("u1", "dev-01")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.enrollDevice("u1", "admin", "dev-01", "MacBook Pro", ecPublicKeyBase64, "ECDSA-P256"));

        assertEquals("DEVICE_ALREADY_EXISTS", ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void createChallenge_success() {
        UserDevice device = new UserDevice("u1", "dev-01", "MacBook Pro", ecPublicKeyBase64, "ECDSA-P256");
        when(deviceRepository.findByIdAndUserId("d1", "u1")).thenReturn(Optional.of(device));
        when(challengeRepository.save(any(DeviceSigningChallenge.class))).thenAnswer(inv -> inv.getArgument(0));

        String payload = "{\"amount\":1000,\"recipient\":\"Supplier A\"}";
        DeviceSigningChallenge challenge = service.createChallenge("u1", "d1", "SUPPLIER_PAYMENT", payload);

        assertNotNull(challenge);
        assertNotNull(challenge.getNonce());
        assertEquals("SUPPLIER_PAYMENT", challenge.getOperationType());
        assertEquals(service.calculateSha256(payload), challenge.getPayloadHash());
        assertTrue(challenge.isPending());
    }

    @Test
    void verifySignature_ecP256_success() throws Exception {
        UserDevice device = new UserDevice("u1", "dev-01", "MacBook Pro", ecPublicKeyBase64, "ECDSA-P256");
        String payload = "{\"amount\":5000,\"account\":\"EG12345\"}";
        String payloadHash = service.calculateSha256(payload);

        DeviceSigningChallenge challenge = new DeviceSigningChallenge(
                "u1", device.getId(), "test-nonce-123", "PAYROLL_DISBURSEMENT", payloadHash, Instant.now().plusSeconds(300)
        );

        when(challengeRepository.findByIdAndUserId("c1", "u1")).thenReturn(Optional.of(challenge));
        when(deviceRepository.findByIdAndUserId(device.getId(), "u1")).thenReturn(Optional.of(device));

        // Sign with private key: data = nonce + ":" + payloadHash
        String signedData = challenge.getNonce() + ":" + challenge.getPayloadHash();
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(ecKeyPair.getPrivate());
        sig.update(signedData.getBytes(StandardCharsets.UTF_8));
        String signatureBase64 = Base64.getEncoder().encodeToString(sig.sign());

        boolean result = service.verifySignature("u1", "finance_user", "c1", signatureBase64, payload);

        assertTrue(result);
        assertEquals(ChallengeStatus.USED, challenge.getStatus());
        verify(auditService).record(eq("DEVICE_SIGNATURE_VERIFIED"), eq("DeviceSigningChallenge"), eq("c1"), eq("finance_user"), anyString(), anyString());
    }

    @Test
    void verifySignature_rsa_success() throws Exception {
        UserDevice device = new UserDevice("u1", "dev-02", "Treasury PC", rsaPublicKeyBase64, "RSA-SHA256");
        String payload = "{\"journalId\":\"JV-001\",\"action\":\"POST\"}";
        String payloadHash = service.calculateSha256(payload);

        DeviceSigningChallenge challenge = new DeviceSigningChallenge(
                "u1", device.getId(), "test-nonce-456", "JOURNAL_POSTING", payloadHash, Instant.now().plusSeconds(300)
        );

        when(challengeRepository.findByIdAndUserId("c2", "u1")).thenReturn(Optional.of(challenge));
        when(deviceRepository.findByIdAndUserId(device.getId(), "u1")).thenReturn(Optional.of(device));

        String signedData = challenge.getNonce() + ":" + challenge.getPayloadHash();
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(rsaKeyPair.getPrivate());
        sig.update(signedData.getBytes(StandardCharsets.UTF_8));
        String signatureBase64 = Base64.getEncoder().encodeToString(sig.sign());

        boolean result = service.verifySignature("u1", "cfo_user", "c2", signatureBase64, payload);

        assertTrue(result);
        assertEquals(ChallengeStatus.USED, challenge.getStatus());
        verify(auditService).record(eq("DEVICE_SIGNATURE_VERIFIED"), eq("DeviceSigningChallenge"), eq("c2"), eq("cfo_user"), anyString(), anyString());
    }

    @Test
    void verifySignature_payloadTampered_throwsException() throws Exception {
        UserDevice device = new UserDevice("u1", "dev-01", "MacBook Pro", ecPublicKeyBase64, "ECDSA-P256");
        String originalPayload = "{\"amount\":5000}";
        String tamperedPayload = "{\"amount\":500000}"; // modified!

        DeviceSigningChallenge challenge = new DeviceSigningChallenge(
                "u1", device.getId(), "nonce-1", "SUPPLIER_PAYMENT", service.calculateSha256(originalPayload), Instant.now().plusSeconds(300)
        );

        when(challengeRepository.findByIdAndUserId("c1", "u1")).thenReturn(Optional.of(challenge));
        when(deviceRepository.findByIdAndUserId(device.getId(), "u1")).thenReturn(Optional.of(device));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.verifySignature("u1", "admin", "c1", "dummy-sig", tamperedPayload));

        assertEquals("SIGNING_PAYLOAD_TAMPERED", ex.getCode());
    }

    @Test
    void verifySignature_replayAttackPrevented_throwsException() {
        UserDevice device = new UserDevice("u1", "dev-01", "MacBook Pro", ecPublicKeyBase64, "ECDSA-P256");
        DeviceSigningChallenge challenge = new DeviceSigningChallenge(
                "u1", device.getId(), "nonce-1", "PAYROLL_DISBURSEMENT", "hash", Instant.now().plusSeconds(300)
        );
        challenge.markUsed(); // already used!

        when(challengeRepository.findByIdAndUserId("c1", "u1")).thenReturn(Optional.of(challenge));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.verifySignature("u1", "admin", "c1", "sig", "payload"));

        assertEquals("SIGNING_CHALLENGE_ALREADY_USED", ex.getCode());
    }

    @Test
    void verifySignature_revokedDevice_throwsException() {
        UserDevice device = new UserDevice("u1", "dev-01", "MacBook Pro", ecPublicKeyBase64, "ECDSA-P256");
        device.revoke("Lost device");

        String payload = "data";
        DeviceSigningChallenge challenge = new DeviceSigningChallenge(
                "u1", device.getId(), "nonce-1", "PAYROLL_DISBURSEMENT", service.calculateSha256(payload), Instant.now().plusSeconds(300)
        );

        when(challengeRepository.findByIdAndUserId("c1", "u1")).thenReturn(Optional.of(challenge));
        when(deviceRepository.findByIdAndUserId(device.getId(), "u1")).thenReturn(Optional.of(device));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.verifySignature("u1", "admin", "c1", "sig", payload));

        assertEquals("DEVICE_REVOKED", ex.getCode());
    }

    @Test
    void verifySignature_invalidCryptoSignature_throwsException() {
        UserDevice device = new UserDevice("u1", "dev-01", "MacBook Pro", ecPublicKeyBase64, "ECDSA-P256");
        String payload = "data";
        DeviceSigningChallenge challenge = new DeviceSigningChallenge(
                "u1", device.getId(), "nonce-1", "PAYROLL_DISBURSEMENT", service.calculateSha256(payload), Instant.now().plusSeconds(300)
        );

        when(challengeRepository.findByIdAndUserId("c1", "u1")).thenReturn(Optional.of(challenge));
        when(deviceRepository.findByIdAndUserId(device.getId(), "u1")).thenReturn(Optional.of(device));

        String invalidSignature = Base64.getEncoder().encodeToString("bad-signature-bytes".getBytes(StandardCharsets.UTF_8));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.verifySignature("u1", "admin", "c1", invalidSignature, payload));

        assertEquals("INVALID_DEVICE_SIGNATURE", ex.getCode());
        verify(auditService).record(eq("DEVICE_SIGNATURE_FAILED"), eq("DeviceSigningChallenge"), eq("c1"), eq("admin"), anyString(), anyString());
    }
}
