package com.bemo.hr.security.pack;

import com.bemo.hr.security.pack.application.IpAllowlistService;
import com.bemo.hr.security.pack.domain.RoleIpAllowlist;
import com.bemo.hr.security.pack.domain.TenantSecuritySettings;
import com.bemo.hr.security.pack.infrastructure.RoleIpAllowlistRepository;
import com.bemo.hr.security.pack.infrastructure.TenantSecuritySettingsRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpAllowlistServiceTests {

    @Mock
    private RoleIpAllowlistRepository ipAllowlistRepository;

    @Mock
    private TenantSecuritySettingsRepository settingsRepository;

    private IpAllowlistService ipAllowlistService;

    private final String appId = "test-app";

    @BeforeEach
    void setUp() {
        ipAllowlistService = new IpAllowlistService(ipAllowlistRepository, settingsRepository);
    }

    @Test
    @DisplayName("CIDR subnet matching helper")
    void testIsIpInCidr() {
        assertTrue(IpAllowlistService.isIpInCidr("192.168.1.50", "192.168.1.0/24"));
        assertFalse(IpAllowlistService.isIpInCidr("192.168.2.1", "192.168.1.0/24"));
        assertTrue(IpAllowlistService.isIpInCidr("10.5.100.20", "10.0.0.0/8"));
        assertTrue(IpAllowlistService.isIpInCidr("127.0.0.1", "127.0.0.1/32"));
        assertFalse(IpAllowlistService.isIpInCidr("127.0.0.2", "127.0.0.1/32"));
    }

    @Test
    @DisplayName("Allowed IP passes role validation")
    void testAllowedIpPasses() {
        RoleIpAllowlist rule = new RoleIpAllowlist(appId, "ADMIN", "192.168.1.0/24", "Office Network");
        when(ipAllowlistRepository.findByAppIdAndRoleCode(appId, "ADMIN")).thenReturn(List.of(rule));

        assertDoesNotThrow(() ->
                ipAllowlistService.validateClientIp(appId, Set.of(RoleCode.ADMIN), "192.168.1.25"));
    }

    @Test
    @DisplayName("Disallowed IP throws IP_NOT_ALLOWED exception")
    void testDisallowedIpThrows() {
        RoleIpAllowlist rule = new RoleIpAllowlist(appId, "ADMIN", "192.168.1.0/24", "Office Network");
        when(ipAllowlistRepository.findByAppIdAndRoleCode(appId, "ADMIN")).thenReturn(List.of(rule));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                ipAllowlistService.validateClientIp(appId, Set.of(RoleCode.ADMIN), "203.0.113.100"));
        assertEquals("IP_NOT_ALLOWED", ex.getCode());
    }

    @Test
    @DisplayName("Super Admin bypass allows access from any IP when enabled")
    void testSuperAdminBypass() {
        TenantSecuritySettings settings = new TenantSecuritySettings(appId);
        settings.setSuperAdminIpBypass(true);
        when(settingsRepository.findByAppId(appId)).thenReturn(Optional.of(settings));

        assertDoesNotThrow(() ->
                ipAllowlistService.validateClientIp(appId, Set.of(RoleCode.SUPER_ADMIN), "203.0.113.100"));
    }
}
