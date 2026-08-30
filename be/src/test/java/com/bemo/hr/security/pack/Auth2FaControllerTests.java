package com.bemo.hr.security.pack;

import com.bemo.hr.security.pack.api.Auth2FaController;
import com.bemo.hr.security.pack.api.SecurityPackApi;
import com.bemo.hr.security.pack.application.TotpService;
import com.bemo.hr.shared.security.AuthApi;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.shared.security.ClientIpResolver;
import com.bemo.hr.shared.security.RefreshCookieCodec;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Auth2FaControllerTests {

    @Mock
    private TotpService totpService;

    @Mock
    private AuthService authService;

    @Mock
    private RefreshCookieCodec refreshCookieCodec;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    private Auth2FaController controller;
    private final String appId = "test-app";

    @BeforeEach
    void setUp() {
        controller = new Auth2FaController(totpService, authService, refreshCookieCodec, clientIpResolver, "bemo_refresh", true);
        TenantContext.set(appId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/auth/2fa/status returns status")
    void testGetStatus() {
        when(authentication.getName()).thenReturn("admin");
        when(authService.getUserIdByUsername(appId, "admin")).thenReturn("u-1");
        when(totpService.getStatus(appId, "u-1")).thenReturn(new TotpService.TotpStatusResult(true, Instant.now(), 8));

        ResponseEntity<SecurityPackApi.TotpStatusResponse> resp = controller.getStatus(authentication);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().enabled());
        assertEquals(8, resp.getBody().remainingBackupCodes());
    }

    @Test
    @DisplayName("POST /api/v1/auth/2fa/enroll returns secret & backup codes")
    void testEnroll() {
        when(authentication.getName()).thenReturn("admin");
        when(authService.getUserIdByUsername(appId, "admin")).thenReturn("u-1");
        when(totpService.enroll(appId, "u-1")).thenReturn(new TotpService.EnrollResult("SECRET32", "otpauth://...", List.of("BC-1")));

        ResponseEntity<SecurityPackApi.TotpEnrollResponse> resp = controller.enroll(authentication);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("SECRET32", resp.getBody().secret());
    }

    @Test
    @DisplayName("POST /api/v1/auth/2fa/verify completes 2FA login")
    void testVerify2fa() {
        SecurityPackApi.TotpVerifyRequest req = new SecurityPackApi.TotpVerifyRequest("challenge-jwt", "123456");
        when(clientIpResolver.resolve(servletRequest)).thenReturn("127.0.0.1");
        AuthApi.LoginResponse loginResp = new AuthApi.LoginResponse("jwt-access", "Bearer", Instant.now().plusSeconds(3600), false, null, null, null);
        AuthService.LoginResult loginResult = new AuthService.LoginResult(appId, loginResp, "refresh-raw", Instant.now().plusSeconds(86400));
        when(authService.verify2faLogin("challenge-jwt", "123456", "dev-1", null, "127.0.0.1")).thenReturn(loginResult);
        when(refreshCookieCodec.encode(appId, "refresh-raw")).thenReturn("encoded-cookie");

        ResponseEntity<AuthApi.LoginResponse> resp = controller.verify2fa(req, servletRequest, servletResponse, "dev-1");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("jwt-access", resp.getBody().accessToken());
    }
}
