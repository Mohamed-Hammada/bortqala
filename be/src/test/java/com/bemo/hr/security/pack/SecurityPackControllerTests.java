package com.bemo.hr.security.pack;

import com.bemo.hr.security.pack.api.SecurityPackApi;
import com.bemo.hr.security.pack.api.SecurityPackController;
import com.bemo.hr.security.pack.application.IpAllowlistService;
import com.bemo.hr.security.pack.application.PasswordPolicyService;
import com.bemo.hr.security.pack.application.TrustedDeviceService;
import com.bemo.hr.security.pack.domain.RoleIpAllowlist;
import com.bemo.hr.security.pack.domain.TenantSecuritySettings;
import com.bemo.hr.security.pack.domain.TrustedDevice;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityPackControllerTests {

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private TrustedDeviceService trustedDeviceService;

    @Mock
    private IpAllowlistService ipAllowlistService;

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    private SecurityPackController controller;
    private final String appId = "test-app";

    @BeforeEach
    void setUp() {
        controller = new SecurityPackController(passwordPolicyService, trustedDeviceService, ipAllowlistService, authService);
        TenantContext.set(appId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/security/policy returns policy")
    void testGetPolicy() {
        TenantSecuritySettings settings = new TenantSecuritySettings(appId);
        when(passwordPolicyService.getOrCreateSettings(appId)).thenReturn(settings);

        ResponseEntity<SecurityPackApi.SecurityPolicyResponse> resp = controller.getPolicy();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(8, resp.getBody().minPasswordLength());
    }

    @Test
    @DisplayName("GET /api/v1/security/devices returns user devices")
    void testGetDevices() {
        when(authentication.getName()).thenReturn("admin");
        when(authService.getUserIdByUsername(appId, "admin")).thenReturn("user-1");
        TrustedDevice dev = new TrustedDevice(appId, "user-1", "d1", "Chrome on Windows", "UA", "127.0.0.1");
        when(trustedDeviceService.listDevices(appId, "user-1")).thenReturn(List.of(dev));

        ResponseEntity<List<SecurityPackApi.TrustedDeviceResponse>> resp = controller.getDevices(authentication);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        assertEquals("d1", resp.getBody().get(0).deviceId());
    }

    @Test
    @DisplayName("POST /api/v1/security/ip-rules adds rule")
    void testCreateIpRule() {
        SecurityPackApi.RoleIpRuleCreateRequest req = new SecurityPackApi.RoleIpRuleCreateRequest("ADMIN", "10.0.0.0/8", "VPN");
        RoleIpAllowlist rule = new RoleIpAllowlist(appId, "ADMIN", "10.0.0.0/8", "VPN");
        when(ipAllowlistService.addRule(appId, "ADMIN", "10.0.0.0/8", "VPN")).thenReturn(rule);

        ResponseEntity<SecurityPackApi.RoleIpRuleResponse> resp = controller.createIpRule(req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("ADMIN", resp.getBody().roleCode());
        assertEquals("10.0.0.0/8", resp.getBody().cidrBlock());
    }
}
