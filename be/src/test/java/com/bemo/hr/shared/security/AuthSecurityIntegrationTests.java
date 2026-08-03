package com.bemo.hr.shared.security;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTests {

    private final AuthService authService;
    private final RefreshCookieCodec refreshCookieCodec;
    private final LoginRateLimiter loginRateLimiter;
    private final LoginStateService loginStateService;
    private final AppUserRepository appUserRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate tx;

    private final List<String> createdUserIds = new ArrayList<>();
    private String appId;
    private String appCode;

    @Autowired
    AuthSecurityIntegrationTests(AuthService authService,
                                 RefreshCookieCodec refreshCookieCodec,
                                 LoginRateLimiter loginRateLimiter,
                                 LoginStateService loginStateService,
                                 AppUserRepository appUserRepository,
                                 TenantApplicationRepository tenantApplicationRepository,
                                 JwtEncoder jwtEncoder,
                                 JwtProperties jwtProperties,
                                 PasswordEncoder passwordEncoder,
                                 MockMvc mockMvc,
                                 JdbcTemplate jdbcTemplate,
                                 PlatformTransactionManager transactionManager) {
        this.authService = authService;
        this.refreshCookieCodec = refreshCookieCodec;
        this.loginRateLimiter = loginRateLimiter;
        this.loginStateService = loginStateService;
        this.appUserRepository = appUserRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void setUp() {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow();
        appId = app.getId();
        appCode = app.getCode();
    }

    @AfterEach
    void cleanup() {
        try {
            appUserRepository.deleteAllById(createdUserIds);
        } finally {
            createdUserIds.clear();
            TenantContext.clear();
        }
    }

    private AppUser loadWithRoles(String userId) {
        return tx.execute(status -> {
            AppUser user = appUserRepository.findById(userId).orElseThrow();
            user.getRoles().size();
            return user;
        });
    }

    private AppUser createUser(String prefix, Set<RoleCode> roles) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + "-" + suffix;
        var request = new AuthApi.UserUpsertRequest(username, "Test " + prefix, "Auth#Test1!",
                roles, null, true, null, true, true, null);
        TenantContext.set(appId);
        var created = authService.create(request);
        createdUserIds.add(created.id());
        return loadWithRoles(created.id());
    }

    private AuthApi.LoginRequest loginRequest(String username, String password) {
        return new AuthApi.LoginRequest(appCode, username, password);
    }

    private String mintAccessToken(AppUser user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(30)))
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("appId", user.getAppId())
                .claim("appCode", appCode)
                .claim("name", user.getDisplayName())
                .claim("tv", user.getTokenVersion())
                .claim("pwc", false)
                .claim("roles", user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void lockedAccountRejectsLoginEvenWithCorrectPassword() {
        AppUser user = createUser("locktest", Set.of(RoleCode.WORKFORCE_MANAGER));
        Instant now = Instant.now();
        for (int i = 0; i < LoginStateService.MAX_LOGIN_ATTEMPTS; i++) {
            loginStateService.recordFailure(appId, user.getUsername(), now);
        }
        AppUser reloaded = appUserRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getFailedLoginAttempts()).isEqualTo(LoginStateService.MAX_LOGIN_ATTEMPTS);
        assertThat(reloaded.isLocked(now)).isTrue();

        assertThatThrownBy(() -> authService.login(loginRequest(user.getUsername(), "Auth#Test1!"), null, "203.0.113." + suffixIp()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("ACCOUNT_TEMPORARILY_LOCKED");
    }

    @Test
    void unknownAppLoginThrowsBadCredentialsWithoutLeakingExistence() {
        String ip = "198.51.100." + suffixIp();
        var request = new AuthApi.LoginRequest("DOES-NOT-EXIST", "ghost", "whatever");
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authService.login(request, "device-x", ip))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials.");
        }
    }

    @Test
    void unknownUserLoginIsThrottledAfterRepeatedFailures() {
        String username = "ghost-" + UUID.randomUUID().toString().substring(0, 8);
        String ip = "192.0.2." + suffixIp();
        var request = loginRequest(username, "wrong-password");
        for (int i = 0; i < LoginRateLimiter.MAX_USERNAME_ATTEMPTS; i++) {
            assertThatThrownBy(() -> authService.login(request, "device-y", ip))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials.");
        }
        assertThat(loginRateLimiter.isTenantBlocked(appId, username, "device-y", ip)).isTrue();
        assertThatThrownBy(() -> authService.login(request, "device-y", ip))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials.");
    }

    @Test
    void globalIpBlockedAfterExcessiveUnknownAppFailures() {
        String ip = "203.0.113." + suffixIp();
        for (int i = 0; i < LoginRateLimiter.MAX_GLOBAL_IP_ATTEMPTS; i++) {
            loginRateLimiter.recordGlobalIpFailure(ip);
        }
        assertThat(loginRateLimiter.isGlobalIpBlocked(ip)).isTrue();
    }

    @Test
    void refreshRotationInvalidatesPreviousTokenAndReuseRevokesFamily() {
        AppUser user = createUser("rotuser", Set.of(RoleCode.WORKFORCE_MANAGER));
        AuthService.LoginResult login = authService.login(
                loginRequest(user.getUsername(), "Auth#Test1!"), "device-r", "198.51.100." + suffixIp());
        String firstCookie = refreshCookieCodec.encode(login.appId(), login.refreshToken());

        AuthService.RefreshResult rotated = authService.refresh(firstCookie, "device-r");
        assertThat(rotated.refreshToken()).isNotEqualTo(login.refreshToken());

        String secondCookie = refreshCookieCodec.encode(rotated.appId(), rotated.refreshToken());
        AuthService.RefreshResult rotatedAgain = authService.refresh(secondCookie, "device-r");
        assertThat(rotatedAgain.refreshToken()).isNotEqualTo(rotated.refreshToken());

        assertThatThrownBy(() -> authService.refresh(secondCookie, "device-r"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("INVALID_REFRESH_TOKEN");

        assertThatThrownBy(() -> authService.refresh(
                refreshCookieCodec.encode(rotatedAgain.appId(), rotatedAgain.refreshToken()), "device-r"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        AppUser user = createUser("logoutusr", Set.of(RoleCode.WORKFORCE_MANAGER));
        AuthService.LoginResult login = authService.login(
                loginRequest(user.getUsername(), "Auth#Test1!"), "device-l", "192.0.2." + suffixIp());
        String cookie = refreshCookieCodec.encode(login.appId(), login.refreshToken());

        authService.logout(cookie);

        assertThatThrownBy(() -> authService.refresh(cookie, "device-l"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void forcedPasswordChangeMustBeClearedAfterChangePassword() {
        AppUser user = createUser("forced", Set.of(RoleCode.WORKFORCE_MANAGER));
        assertThat(user.isMustChangePassword()).isTrue();

        AuthService.LoginResult login = authService.login(
                loginRequest(user.getUsername(), "Auth#Test1!"), "device-c", "203.0.113." + suffixIp());
        assertThat(login.response().mustChangePassword()).isTrue();

        TenantContext.set(appId);
        assertThatThrownBy(() -> authService.changePassword(user.getUsername(),
                new AuthApi.ChangePasswordRequest("wrong-current", "NewAuth#Test1")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("PASSWORD_MISMATCH");

        authService.changePassword(user.getUsername(),
                new AuthApi.ChangePasswordRequest("Auth#Test1!", "NewAuth#Test1"));

        AppUser after = appUserRepository.findById(user.getId()).orElseThrow();
        assertThat(after.isMustChangePassword()).isFalse();
        assertThat(passwordEncoder.matches("NewAuth#Test1", after.getPasswordHash())).isTrue();

        assertThatThrownBy(() -> authService.refresh(
                refreshCookieCodec.encode(appId, login.refreshToken()), "device-c"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("INVALID_REFRESH_TOKEN");

        AuthService.LoginResult reLogin = authService.login(
                loginRequest(user.getUsername(), "NewAuth#Test1"), "device-c", "203.0.113." + suffixIp());
        assertThat(reLogin.response().mustChangePassword()).isFalse();
    }

    @Test
    void onlySuperAdminCanChangeDashboardCustomizationPolicyForAdmins() {
        TenantApplication app = tenantApplicationRepository.findById(appId).orElseThrow();
        boolean original = app.isAdminDashboardCustomizationEnabled();
        TenantContext.set(appId);
        var admin = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, "admin").orElseThrow();
        var superAdmin = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, "superadmin").orElseThrow();
        var current = authService.currentAppSettings();
        var request = new AuthApi.AppSettingsRequest(
                current.sessionTimeoutMinutes(), current.sessionTimeoutEnabled(), current.showReportPresets(),
                current.attendanceAnomalyThresholdPercent(), current.automaticProcurementNumbering(),
                !current.adminDashboardCustomizationEnabled(),
                current.minPasswordLength(), current.requireUppercase(), current.requireLowercase(),
                current.requireNumbers(), current.requireSpecialChars(), current.disallowSpaces(),
                current.maxPasswordLength(), current.passwordExpiryDays(), current.passwordHistoryCount());

        TenantContext.set(appId);
        assertThatThrownBy(() -> authService.updateAppSettings(request, admin.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Super Admin");

        authService.updateAppSettings(request, superAdmin.getUsername());
        TenantApplication updated = tenantApplicationRepository.findById(appId).orElseThrow();
        assertThat(updated.isAdminDashboardCustomizationEnabled()).isNotEqualTo(original);

        authService.updateAppSettings(new AuthApi.AppSettingsRequest(
                current.sessionTimeoutMinutes(), current.sessionTimeoutEnabled(), current.showReportPresets(),
                current.attendanceAnomalyThresholdPercent(), current.automaticProcurementNumbering(),
                original, current.minPasswordLength(), current.requireUppercase(), current.requireLowercase(),
                current.requireNumbers(), current.requireSpecialChars(), current.disallowSpaces(),
                current.maxPasswordLength(), current.passwordExpiryDays(), current.passwordHistoryCount()),
                superAdmin.getUsername());
    }

    @Test
    void workforceEndpointsRequireWorkforceOrAdminRoles() throws Exception {
        AppUser workforce = createUser("wfuser", Set.of(RoleCode.WORKFORCE_MANAGER));
        AppUser finance = createUser("finuser", Set.of(RoleCode.FINANCE_MANAGER));

        mockMvc.perform(get("/api/v1/workforce/contractors")
                        .header("Authorization", "Bearer " + mintAccessToken(workforce)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/workforce/contractors")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointsRequireSuperAdminOrAdminRoles() throws Exception {
        AppUser finance = createUser("finuser2", Set.of(RoleCode.FINANCE_MANAGER));
        AppUser superAdmin = loadWithRoles(
                appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, "superadmin").orElseThrow().getId());

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrManagerReceivesOnlyWorkforceRolesFromCorrectiveMigration() throws Exception {
        AppUser hrManager = createUser("hrgrant", Set.of(RoleCode.HR_MANAGER));
        jdbcTemplate.execute("""
                INSERT INTO user_roles (user_id, role_code)
                SELECT ur.user_id, r.code
                FROM user_roles ur
                CROSS JOIN (VALUES
                    ('WORKFORCE_MANAGER'),('WORKFORCE_REVIEWER')
                ) AS r(code)
                WHERE ur.role_code = 'HR_MANAGER'
                  AND NOT EXISTS (SELECT 1 FROM user_roles x WHERE x.user_id = ur.user_id AND x.role_code = r.code)
                """);

        AppUser reloaded = loadWithRoles(hrManager.getId());
        Set<RoleCode> roles = reloaded.getRoles().stream().map(Role::getCode).collect(java.util.stream.Collectors.toSet());
        assertThat(roles).contains(RoleCode.WORKFORCE_MANAGER, RoleCode.WORKFORCE_REVIEWER);
        assertThat(roles).doesNotContain(RoleCode.PAYROLL_MANAGER, RoleCode.WORKFORCE_FINANCE);

        mockMvc.perform(get("/api/v1/workforce/contractors")
                        .header("Authorization", "Bearer " + mintAccessToken(reloaded)))
                .andExpect(status().isOk());
    }

    private int suffixIp() {
        return 1 + (int) (Math.random() * 250);
    }
}
