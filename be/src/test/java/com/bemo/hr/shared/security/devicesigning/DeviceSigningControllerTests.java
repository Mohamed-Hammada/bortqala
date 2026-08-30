package com.bemo.hr.shared.security.devicesigning;

import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceSigningControllerTests {

    @Mock
    private DeviceSigningService deviceSigningService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private Authentication authentication;

    private DeviceSigningController controller;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        controller = new DeviceSigningController(deviceSigningService, appUserRepository);
        testUser = new AppUser("test-app", "testuser", "Test User", "encoded-pass", java.util.Set.of(), java.util.Set.of(), true, true);
        when(authentication.getName()).thenReturn("testuser");
        when(appUserRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    void listDevices_returnsList() {
        UserDevice device = new UserDevice(testUser.getId(), "dev-01", "MacBook", "pubkey", "ECDSA-P256");
        when(deviceSigningService.listUserDevices(testUser.getId())).thenReturn(List.of(device));

        DeviceSigningApi.DeviceListResponse response = controller.listDevices(authentication);

        assertNotNull(response);
        assertEquals(1, response.devices().size());
        assertEquals("dev-01", response.devices().get(0).deviceIdentifier());
    }

    @Test
    void enrollDevice_returnsCreatedDevice() {
        UserDevice device = new UserDevice(testUser.getId(), "dev-01", "MacBook", "pubkey", "ECDSA-P256");
        when(deviceSigningService.enrollDevice(eq(testUser.getId()), eq("testuser"), eq("dev-01"), eq("MacBook"), eq("pubkey"), eq("ECDSA-P256")))
                .thenReturn(device);

        DeviceSigningApi.EnrollDeviceRequest request = new DeviceSigningApi.EnrollDeviceRequest("dev-01", "MacBook", "pubkey", "ECDSA-P256");
        DeviceSigningApi.DeviceResponse response = controller.enrollDevice(request, authentication);

        assertNotNull(response);
        assertEquals("dev-01", response.deviceIdentifier());
        assertEquals("MacBook", response.deviceName());
    }

    @Test
    void revokeDevice_delegatesToService() {
        controller.revokeDevice("dev-id-1", new DeviceSigningApi.RevokeDeviceRequest("Old phone"), authentication);

        verify(deviceSigningService).revokeDevice(eq(testUser.getId()), eq("testuser"), eq("dev-id-1"), eq("Old phone"));
    }

    @Test
    void createChallenge_returnsChallengeDto() {
        DeviceSigningChallenge challenge = new DeviceSigningChallenge(
                testUser.getId(), "dev-id-1", "nonce-123", "SUPPLIER_PAYMENT", "hash-456", Instant.now().plusSeconds(300)
        );
        when(deviceSigningService.createChallenge(eq(testUser.getId()), eq("dev-id-1"), eq("SUPPLIER_PAYMENT"), anyString()))
                .thenReturn(challenge);

        DeviceSigningApi.CreateChallengeRequest request = new DeviceSigningApi.CreateChallengeRequest("dev-id-1", "SUPPLIER_PAYMENT", "payload-data");
        DeviceSigningApi.ChallengeResponse response = controller.createChallenge(request, authentication);

        assertNotNull(response);
        assertEquals("nonce-123", response.nonce());
        assertEquals("SUPPLIER_PAYMENT", response.operationType());
    }

    @Test
    void verifySignature_returnsVerificationResponse() {
        when(deviceSigningService.verifySignature(eq(testUser.getId()), eq("testuser"), eq("chal-1"), eq("sig-val"), eq("payload-data")))
                .thenReturn(true);

        DeviceSigningApi.VerifySignatureRequest request = new DeviceSigningApi.VerifySignatureRequest("chal-1", "sig-val", "payload-data");
        DeviceSigningApi.VerificationResponse response = controller.verifySignature(request, authentication);

        assertNotNull(response);
        assertTrue(response.verified());
        assertEquals("chal-1", response.challengeId());
    }
}
