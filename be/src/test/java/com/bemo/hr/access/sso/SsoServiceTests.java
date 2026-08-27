package com.bemo.hr.access.sso;

import com.bemo.hr.access.sso.application.SsoApi;
import com.bemo.hr.access.sso.application.SsoService;
import com.bemo.hr.access.sso.domain.SsoConfig;
import com.bemo.hr.access.sso.domain.SsoConfigRepository;
import com.bemo.hr.access.sso.domain.UserSsoIdentity;
import com.bemo.hr.access.sso.domain.UserSsoIdentityRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SsoServiceTests {

    @Mock private SsoConfigRepository ssoConfigRepository;
    @Mock private UserSsoIdentityRepository userSsoIdentityRepository;
    @Mock private com.bemo.hr.audit.application.AuditService auditService;

    @InjectMocks
    private SsoService ssoService;

    private static final String TEST_APP_ID = "DEMO";

    @BeforeEach
    void setUp() { TenantContext.set(TEST_APP_ID); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void createConfig_validRequest_persists() {
        var request = new SsoApi.CreateConfigRequest("GOOGLE", "client123", "secret",
                "https://accounts.google.com", "https://accounts.google.com/.well-known/openid-configuration",
                true, "VIEWER");
        when(ssoConfigRepository.save(any(SsoConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = ssoService.createConfig(request);

        assertEquals("GOOGLE", result.getProvider().name());
        assertEquals("client123", result.getClientId());
        assertTrue(result.isAutoProvision());
        verify(ssoConfigRepository).save(any(SsoConfig.class));
    }

    @Test
    void listConfigs_returnsActiveConfigsForApp() {
        var config = new SsoConfig(TEST_APP_ID, SsoConfig.Provider.GOOGLE, "c", "s",
                "issuer", null, true, "VIEWER");
        when(ssoConfigRepository.findByAppIdAndActiveTrue(TEST_APP_ID)).thenReturn(List.of(config));

        var result = ssoService.listConfigs();

        assertEquals(1, result.size());
    }

    @Test
    void hasActiveConfig_existingProvider_returnsTrue() {
        var config = new SsoConfig(TEST_APP_ID, SsoConfig.Provider.GOOGLE, "c", "s",
                "issuer", null, true, "VIEWER");
        when(ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(TEST_APP_ID, "GOOGLE"))
                .thenReturn(Optional.of(config));

        assertTrue(ssoService.hasActiveConfig("GOOGLE"));
    }

    @Test
    void hasActiveConfig_missingProvider_returnsFalse() {
        when(ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(TEST_APP_ID, "MICROSOFT"))
                .thenReturn(Optional.empty());

        assertFalse(ssoService.hasActiveConfig("MICROSOFT"));
    }

    @Test
    void startAuth_validConfig_returnsAuthorizationUrl() {
        var config = new SsoConfig(TEST_APP_ID, SsoConfig.Provider.GOOGLE, "client123", "secret",
                "https://accounts.google.com", "https://accounts.google.com/.well-known/openid-configuration",
                true, "VIEWER");
        when(ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(TEST_APP_ID, "GOOGLE"))
                .thenReturn(Optional.of(config));

        var result = ssoService.startAuth("GOOGLE");

        assertNotNull(result.authorizationUrl());
        assertNotNull(result.stateToken());
        assertTrue(result.authorizationUrl().contains("client_id=client123"));
    }

    @Test
    void startAuth_noConfig_throwsNotFound() {
        when(ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(TEST_APP_ID, "GOOGLE"))
                .thenReturn(Optional.empty());

        assertThrows(com.bemo.hr.shared.domain.NotFoundException.class,
                () -> ssoService.startAuth("GOOGLE"));
    }

    @Test
    void handleCallback_validState_newUser_provisions() {
        String stateToken = createValidStateToken();
        when(userSsoIdentityRepository.findByAppIdAndProviderAndSubject(
                org.mockito.ArgumentMatchers.eq(TEST_APP_ID),
                org.mockito.ArgumentMatchers.eq("GOOGLE"),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        when(userSsoIdentityRepository.save(org.mockito.ArgumentMatchers.any(UserSsoIdentity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        var config = new SsoConfig(TEST_APP_ID, SsoConfig.Provider.GOOGLE, "c", "s",
                "issuer", null, true, "VIEWER");
        when(ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(TEST_APP_ID, "GOOGLE"))
                .thenReturn(Optional.of(config));

        var result = ssoService.handleCallback(stateToken, "GOOGLE", "authcode123");

        assertTrue(result.newlyProvisioned());
        assertNotNull(result.userId());
    }

    @Test
    void handleCallback_invalidState_throwsInvalid() {
        assertThrows(BusinessRuleException.class,
                () -> ssoService.handleCallback("invalid", "GOOGLE", "code"));
    }

    @Test
    void handleCallback_expiredState_throwsExpired() {
        String expiredToken = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("state:nonce:" + (System.currentTimeMillis() - 600_000)).getBytes());

        assertThrows(BusinessRuleException.class,
                () -> ssoService.handleCallback(expiredToken, "GOOGLE", "code"));
    }

    @Test
    void handleCallback_unknownEmail_noAutoProvision_throws() {
        String stateToken = createValidStateToken();
        var config = new SsoConfig(TEST_APP_ID, SsoConfig.Provider.GOOGLE, "c", "s",
                "issuer", null, false, "VIEWER");
        when(ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(TEST_APP_ID, "GOOGLE"))
                .thenReturn(Optional.of(config));
        when(userSsoIdentityRepository.findByAppIdAndProviderAndSubject(
                org.mockito.ArgumentMatchers.eq(TEST_APP_ID),
                org.mockito.ArgumentMatchers.eq("GOOGLE"),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        var ex = assertThrows(BusinessRuleException.class,
                () -> ssoService.handleCallback(stateToken, "GOOGLE", "code"));
        assertEquals("SSO_UNKNOWN_EMAIL", ex.getCode());
    }

    @Test
    void handleCallback_disabledProvider_throws() {
        String stateToken = createValidStateToken();
        when(ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(TEST_APP_ID, "GOOGLE"))
                .thenReturn(Optional.empty());

        var ex = assertThrows(BusinessRuleException.class,
                () -> ssoService.handleCallback(stateToken, "GOOGLE", "code"));
        assertEquals("SSO_PROVIDER_DISABLED", ex.getCode());
    }

    @Test
    void configResponse_mapsFieldsAndHidesSecret() {
        var config = new SsoConfig(TEST_APP_ID, SsoConfig.Provider.MICROSOFT, "ms-client", "supersecret",
                "https://login.microsoftonline.com", null, false, "ADMIN");
        config.setActive(false);

        var response = SsoApi.ConfigResponse.from(config);

        assertEquals("MICROSOFT", response.provider());
        assertEquals("ms-client", response.clientId());
        assertFalse(response.autoProvision());
        assertEquals("ADMIN", response.defaultRole());
        assertFalse(response.active());
    }

    private String createValidStateToken() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("state:nonce:" + System.currentTimeMillis()).getBytes());
    }
}
