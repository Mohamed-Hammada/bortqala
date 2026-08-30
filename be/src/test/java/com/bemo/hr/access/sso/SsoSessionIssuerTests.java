package com.bemo.hr.access.sso;

import com.bemo.hr.access.sso.application.SsoApi;
import com.bemo.hr.access.sso.application.SsoSessionIssuer;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.JwtProperties;
import com.bemo.hr.shared.security.RefreshTokenService;
import com.bemo.hr.shared.security.Role;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SsoSessionIssuerTests {

    private static final String SECRET = "sso-test-secret-0123456789abcdef0123456789abcdef";
    private static final String APP_ID = "DEMO";

    @Mock private TenantApplicationRepository tenantApplicationRepository;
    @Mock private RefreshTokenService refreshTokenService;

    @Test
    void issue_producesDecodableJwtWithRolesAndRefreshToken() {
        JwtProperties properties = new JwtProperties(SECRET, "https://issuer.example", Duration.ofMinutes(15), Duration.ofDays(30));
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                        new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();

        TenantApplication app = org.mockito.Mockito.mock(TenantApplication.class);
        when(app.getId()).thenReturn(APP_ID);
        when(app.getCode()).thenReturn("DEMO-CODE");
        when(app.isSessionTimeoutEnabled()).thenReturn(false);
        when(tenantApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        AppUser user = new AppUser(APP_ID, "sso@demo", "SSO User", "hash",
                new LinkedHashSet<>(Set.of(new Role(RoleCode.VIEWER, "Viewer"), new Role(RoleCode.HR_REVIEWER, "HR Reviewer"))),
                Set.of("dashboard"), true, true);
        user.markPasswordChanged(Instant.now());
        when(refreshTokenService.issue(eq(APP_ID), eq(user.getId()), eq("sso")))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("raw-refresh", "rt-id", Instant.now().plus(Duration.ofDays(30))));

        SsoSessionIssuer issuer = new SsoSessionIssuer(encoder, properties, tenantApplicationRepository, refreshTokenService);
        Instant now = Instant.now();
        SsoApi.CallbackResult result = issuer.issue(user, true, now);

        assertEquals(user.getId(), result.userId());
        assertTrue(result.newlyProvisioned());
        assertEquals("raw-refresh", result.refreshToken());
        assertEquals(900, result.expiresInSeconds());
        assertEquals("sso@demo", result.username());
        assertEquals(List.of("HR_REVIEWER", "VIEWER"), result.roles());

        var jwt = decoder.decode(result.accessToken());
        assertEquals("https://issuer.example", jwt.getIssuer().toExternalForm());
        assertEquals("sso@demo", jwt.getSubject());
        assertEquals(APP_ID, jwt.getClaimAsString("appId"));
        assertEquals("DEMO-CODE", jwt.getClaimAsString("appCode"));
        assertEquals(user.getId(), jwt.getClaimAsString("userId"));
        assertEquals(user.getTokenVersion(), ((Number) jwt.getClaim("tv")).intValue());
        assertEquals(List.of("HR_REVIEWER", "VIEWER"), jwt.getClaimAsStringList("roles"));

        verify(refreshTokenService).issue(eq(APP_ID), eq(user.getId()), eq("sso"));
    }

    @Test
    void issue_sessionTimeoutEnabled_overridesTtl() {
        JwtProperties properties = new JwtProperties(SECRET, "https://issuer.example", Duration.ofMinutes(60), Duration.ofDays(30));
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

        TenantApplication app = org.mockito.Mockito.mock(TenantApplication.class);
        when(app.getId()).thenReturn(APP_ID);
        when(app.getCode()).thenReturn("DEMO-CODE");
        when(app.isSessionTimeoutEnabled()).thenReturn(true);
        when(app.getSessionTimeoutMinutes()).thenReturn(10);
        when(tenantApplicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        AppUser user = new AppUser(APP_ID, "sso@demo", "SSO User", "hash",
                new LinkedHashSet<>(Set.of(new Role(RoleCode.VIEWER, "Viewer"))),
                Set.of("dashboard"), true, true);
        when(refreshTokenService.issue(eq(APP_ID), eq(user.getId()), eq("sso")))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("raw-refresh", "rt-id", Instant.now().plus(Duration.ofDays(30))));

        SsoSessionIssuer issuer = new SsoSessionIssuer(encoder, properties, tenantApplicationRepository, refreshTokenService);
        SsoApi.CallbackResult result = issuer.issue(user, false, Instant.now());

        assertEquals(600, result.expiresInSeconds());
    }
}