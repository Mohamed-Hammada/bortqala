package com.bemo.hr.security.pack;

import com.bemo.hr.security.pack.application.TrustedDeviceService;
import com.bemo.hr.security.pack.domain.TrustedDevice;
import com.bemo.hr.security.pack.infrastructure.TrustedDeviceRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustedDeviceServiceTests {

    @Mock
    private TrustedDeviceRepository trustedDeviceRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private TrustedDeviceService trustedDeviceService;

    private final String appId = "test-app";
    private final String userId = "test-user";

    @BeforeEach
    void setUp() {
        trustedDeviceService = new TrustedDeviceService(trustedDeviceRepository, appUserRepository);
    }

    @Test
    @DisplayName("Record new device activity saves device")
    void testRecordNewDeviceActivity() {
        when(trustedDeviceRepository.findByAppIdAndUserIdAndDeviceId(appId, userId, "dev-1"))
                .thenReturn(Optional.empty());

        trustedDeviceService.recordDeviceActivity(appId, userId, "dev-1", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", "192.168.1.10");

        verify(trustedDeviceRepository).save(any(TrustedDevice.class));
    }

    @Test
    @DisplayName("Revoke device marks revoked and bumps user tokenVersion")
    void testRevokeDevice() {
        String recordId = "rec-1";
        TrustedDevice device = new TrustedDevice(appId, userId, "dev-1", "Chrome on Windows", "UA", "127.0.0.1");
        when(trustedDeviceRepository.findById(recordId)).thenReturn(Optional.of(device));

        AppUser user = new AppUser(appId, "admin", "Admin", "hash", java.util.Set.of(), java.util.Set.of(), true, true);
        long initialVersion = user.getTokenVersion();
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

        trustedDeviceService.revokeDevice(appId, userId, recordId);

        assertTrue(device.isRevoked());
        assertNotNull(device.getRevokedAt());
        assertEquals(initialVersion + 1, user.getTokenVersion());
        verify(appUserRepository).save(user);
    }

    @Test
    @DisplayName("Revoking already revoked device throws exception")
    void testRevokingAlreadyRevokedThrows() {
        String recordId = "rec-1";
        TrustedDevice device = new TrustedDevice(appId, userId, "dev-1", "Chrome on Windows", "UA", "127.0.0.1");
        device.revoke();
        when(trustedDeviceRepository.findById(recordId)).thenReturn(Optional.of(device));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> trustedDeviceService.revokeDevice(appId, userId, recordId));
        assertEquals("DEVICE_ALREADY_REVOKED", ex.getCode());
    }
}
