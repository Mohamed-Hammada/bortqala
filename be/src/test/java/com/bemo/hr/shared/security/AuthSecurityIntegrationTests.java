package com.bemo.hr.shared.security;

import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private final TenantFeatureRepository tenantFeatureRepository;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate tx;
    private final AuditLogRepository auditLogRepository;

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
                                 TenantFeatureRepository tenantFeatureRepository,
                                 JwtEncoder jwtEncoder,
                                 JwtProperties jwtProperties,
                                 PasswordEncoder passwordEncoder,
                                 MockMvc mockMvc,
                                 JdbcTemplate jdbcTemplate,
                                 PlatformTransactionManager transactionManager,
                                 AuditLogRepository auditLogRepository) {
        this.authService = authService;
        this.refreshCookieCodec = refreshCookieCodec;
        this.loginRateLimiter = loginRateLimiter;
        this.loginStateService = loginStateService;
        this.appUserRepository = appUserRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.tenantFeatureRepository = tenantFeatureRepository;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.tx = new TransactionTemplate(transactionManager);
        this.auditLogRepository = auditLogRepository;
    }

    @BeforeEach
    void setUp() {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow();
        appId = app.getId();
        appCode = app.getCode();
        TenantContext.set(appId);
        enableContractorAccountsFeature();
        enablePayrollFeature();
    }

    private void enableContractorAccountsFeature() {
        enableFeature("workforce.contractorAccounts.enabled");
    }

    private void enablePayrollFeature() {
        enableFeature("payroll.enabled");
    }

    private void enableFeature(String featureKey) {
        var featureId = new TenantFeatureId(appId, featureKey);
        var feature = tenantFeatureRepository.findById(featureId)
                .orElseGet(() -> new TenantFeature(appId, featureKey, true, null, "auth-security-tests"));
        feature.setEnabled(true);
        tenantFeatureRepository.save(feature);
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
        return createUser(prefix, roles, true);
    }

    private AppUser createUser(String prefix, Set<RoleCode> roles, boolean canViewSalary) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + "-" + suffix;
        var request = new AuthApi.UserUpsertRequest(username, "Test " + prefix, "Auth#Test1!",
                roles, null, canViewSalary, null, true, true, null, null);
        TenantContext.set(appId);
        var created = authService.create(request, "admin");
        createdUserIds.add(created.id());
        return loadWithRoles(created.id());
    }

    private AuthApi.LoginRequest loginRequest(String username, String password) {
        return new AuthApi.LoginRequest(appCode, username, password);
    }

    private String mintAccessToken(AppUser user) {
        return mintToken(baseClaims(user));
    }

    private String mintToken(JwtClaimsSet.Builder claims) {
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    private JwtClaimsSet.Builder baseClaims(AppUser user) {
        Instant now = Instant.now();
        return JwtClaimsSet.builder()
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
                .claim("roles", user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList());
    }

    @Test
    void lockedAccountRejectsLoginEvenWithCorrectPassword() {
        AppUser user = createUser("locktest", Set.of(RoleCode.WORKFORCE_MANAGER));
        appUserRepository.saveAndFlush(user);
        Instant now = Instant.now();
        for (int i = 0; i < LoginStateService.MAX_LOGIN_ATTEMPTS; i++) {
            loginStateService.recordFailure(appId, user.getUsername(), now);
        }

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
                current.automaticDocumentNumbering(),
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
                current.automaticDocumentNumbering(),
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

    @Test
    void payrollManagerWithoutSalaryPermissionIsForbiddenFromPayrollSheet() throws Exception {
        AppUser restricted = createUser("payrestrict", Set.of(RoleCode.PAYROLL_MANAGER), false);
        AppUser allowed = createUser("payallowed", Set.of(RoleCode.PAYROLL_MANAGER), true);

        mockMvc.perform(get("/api/v1/payroll")
                        .param("year", "2026").param("month", "8")
                        .header("Authorization", "Bearer " + mintAccessToken(restricted)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/payroll")
                        .param("year", "2026").param("month", "8")
                        .header("Authorization", "Bearer " + mintAccessToken(allowed)))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminCanViewPayrollRegardlessOfSalaryPermissionFlag() throws Exception {
        AppUser superAdmin = loadWithRoles(
                appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, "superadmin").orElseThrow().getId());

        mockMvc.perform(get("/api/v1/payroll")
                        .param("year", "2026").param("month", "8")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void viewerWithSalaryFlagCannotReadPayroll() throws Exception {
        AppUser viewer = createUser("viewersal", Set.of(RoleCode.VIEWER), true);

        mockMvc.perform(get("/api/v1/payroll")
                        .param("year", "2026").param("month", "8")
                        .header("Authorization", "Bearer " + mintAccessToken(viewer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeManagerWithSalaryFlagCannotReadPayroll() throws Exception {
        AppUser finance = createUser("finsal", Set.of(RoleCode.FINANCE_MANAGER), true);

        mockMvc.perform(get("/api/v1/payroll")
                        .param("year", "2026").param("month", "8")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());
    }

    @Test
    void payrollManagerWithoutSalaryFlagCannotCallSalaryReturningEndpoints() throws Exception {
        AppUser restricted = createUser("payrestrict2", Set.of(RoleCode.PAYROLL_MANAGER), false);
        String token = mintAccessToken(restricted);

        mockMvc.perform(get("/api/v1/payroll")
                        .param("year", "2026").param("month", "8")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/payroll/export")
                        .param("year", "2026").param("month", "8")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payroll/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"emp-1\",\"periodYear\":2026,\"periodMonth\":8,\"expectedVersion\":0}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payroll/pay-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodYear\":2026,\"periodMonth\":8}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payroll/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodYear\":2026,\"periodMonth\":8,\"targetStatus\":\"PAID\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payroll/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"p-1\",\"reason\":\"test\",\"expectedVersion\":0}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void demoLoginIsUnavailableWhenFeatureIsDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"whatever\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEMO_NO_LOGIN_LINK_INVALID"));
    }

    @Test
    void tokenWithForeignIssuerIsRejected() throws Exception {
        AppUser superAdmin = loadWithRoles(
                appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, "superadmin").orElseThrow().getId());

        String token = mintToken(baseClaims(superAdmin).issuer("attacker-issuer"));

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenIsInvalidatedAfterPasswordChange() throws Exception {
        AppUser user = createUser("pwrevoke", Set.of(RoleCode.WORKFORCE_MANAGER));
        String before = mintAccessToken(user);

        TenantContext.set(appId);
        authService.changePassword(user.getUsername(),
                new AuthApi.ChangePasswordRequest("Auth#Test1!", "Rotated#Auth1"));
        AppUser after = loadWithRoles(user.getId());

        mockMvc.perform(get("/api/v1/workforce/contractors")
                        .header("Authorization", "Bearer " + mintAccessToken(after)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/workforce/contractors")
                        .header("Authorization", "Bearer " + before))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reportEndpointsRequireHrOrAdminRoles() throws Exception {
        AppUser reviewer = createUser("rptreviewer", Set.of(RoleCode.HR_REVIEWER));
        AppUser finance = createUser("rptfinance", Set.of(RoleCode.FINANCE_MANAGER));

        mockMvc.perform(get("/api/v1/reports")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/reports/does-not-exist/export")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reportReopenRequiresAdminOrHrManagerRole() throws Exception {
        AppUser reviewer = createUser("reopreviewer", Set.of(RoleCode.HR_REVIEWER));
        AppUser admin = loadWithRoles(
                appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, "admin").orElseThrow().getId());

        mockMvc.perform(post("/api/v1/reports/does-not-exist/reopen")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports/does-not-exist/reopen")
                        .header("Authorization", "Bearer " + mintAccessToken(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void importEndpointsRequireHrOrAdminRoles() throws Exception {
        AppUser reviewer = createUser("impviewer", Set.of(RoleCode.HR_REVIEWER));
        AppUser finance = createUser("impfinance", Set.of(RoleCode.FINANCE_MANAGER));

        mockMvc.perform(get("/api/v1/imports")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/imports")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/imports/unmatched")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/imports/unmatched")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());
    }

    @Test
    void biometricDeviceListRequiresManagementRoles() throws Exception {
        AppUser reviewer = createUser("devreviewer", Set.of(RoleCode.HR_REVIEWER));
        AppUser manager = createUser("devmanager", Set.of(RoleCode.HR_MANAGER));
        AppUser finance = createUser("devfinance", Set.of(RoleCode.FINANCE_MANAGER));

        mockMvc.perform(get("/api/v1/imports/devices")
                        .header("Authorization", "Bearer " + mintAccessToken(manager)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/imports/devices")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/imports/devices")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());
    }

    @Test
    void importUploadRequiresHrOrAdminRoles() throws Exception {
        AppUser reviewer = createUser("upreviewer", Set.of(RoleCode.HR_REVIEWER));
        AppUser finance = createUser("upfinance", Set.of(RoleCode.FINANCE_MANAGER));
        byte[] csv = ("Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
                + "EMP-101,2026-07-24,08:00,16:00,08:07,16:15\n").getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "attendance.csv", "text/csv", csv);
        String sourceId = "src-" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update("""
                INSERT INTO biometric_sources (id, app_id, source_type, name, normalized_code, active, created_at)
                VALUES (?, ?, 'FILE_DEVICE', 'Test Source', 'test_source', TRUE, CURRENT_TIMESTAMP)
                """, sourceId, appId);

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("sourceId", "missing-source")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("sourceId", sourceId)
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer)))
                .andExpect(status().isCreated());
    }

    @Test
    void disabledPayrollFeatureReturnsForbiddenForPayrollEndpoints() throws Exception {
        AppUser payroll = createUser("featpay", Set.of(RoleCode.PAYROLL_MANAGER), true);
        TenantFeature payrollFeature = enableFeatureRow("payroll.enabled", false);
        try {
            mockMvc.perform(get("/api/v1/payroll")
                            .param("year", "2026").param("month", "8")
                            .header("Authorization", "Bearer " + mintAccessToken(payroll)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FEATURE_DISABLED"));
        } finally {
            payrollFeature.setEnabled(true);
            tenantFeatureRepository.save(payrollFeature);
        }
    }

    @Test
    void disabledProcurementFeatureReturnsForbiddenForProcurementEndpoints() throws Exception {
        AppUser manager = createUser("featproc", Set.of(RoleCode.WORKFORCE_MANAGER));
        TenantFeature procurementFeature = enableFeatureRow("procurement.enabled", false);
        try {
            mockMvc.perform(get("/api/v1/trade/procurement/orders")
                            .header("Authorization", "Bearer " + mintAccessToken(manager)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FEATURE_DISABLED"));
        } finally {
            procurementFeature.setEnabled(true);
            tenantFeatureRepository.save(procurementFeature);
        }
    }

    @Test
    void accessValidateEndpointHandlesNewEditMissingAndSelfAssignments() throws Exception {
        AppUser superAdmin = loadAccount("superadmin");
        AppUser target = createUser("valtarget", Set.of(RoleCode.VIEWER));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(superAdmin.getId()));

        mockMvc.perform(post("/api/v1/users/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCodes\":[\"ADMIN\"],\"menuCodes\":[\"payroll\"]}")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCodes":["ADMIN"],"menuCodes":["payroll"],"targetUserId":"%s"}
                                """.formatted(target.getId()))
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCodes":["ADMIN"],"menuCodes":["payroll"],"targetUserId":"does-not-exist"}
                                """.formatted())
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/users/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleCodes":["FINANCE_MANAGER"],"menuCodes":[],"targetUserId":"%s"}
                                """.formatted(target.getId()))
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminBypassesTenantFeatureGatesWhileOthersRespectThem() throws Exception {
        AppUser superAdmin = loadAccount("superadmin");
        AppUser admin = loadAccount("admin");
        enableFeatureRow("payroll.enabled", false);

        // SUPER_ADMIN must not be blocked by a tenant-disabled feature: the
        // validate flow reports no FEATURE_DISABLED error.
        mockMvc.perform(post("/api/v1/users/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCodes\":[\"PAYROLL_MANAGER\"],\"menuCodes\":[\"payroll\"]}")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // Non-owner roles still respect the tenant feature configuration.
        mockMvc.perform(post("/api/v1/users/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCodes\":[\"PAYROLL_MANAGER\"],\"menuCodes\":[\"payroll\"]}")
                        .header("Authorization", "Bearer " + mintAccessToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[?(@.code == 'ACCESS_FEATURE_DISABLED')]").exists());

        // SUPER_ADMIN still reaches the interceptor-gated payroll endpoint.
        mockMvc.perform(get("/api/v1/payroll")
                        .param("year", "2026")
                        .param("month", "1")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk());
    }

    private TenantFeature enableFeatureRow(String featureKey, boolean enabled) {
        var featureId = new TenantFeatureId(appId, featureKey);
        var feature = tenantFeatureRepository.findById(featureId)
                .orElseGet(() -> new TenantFeature(appId, featureKey, enabled, null, "auth-security-tests"));
        feature.setEnabled(enabled);
        return tenantFeatureRepository.save(feature);
    }

    private int suffixIp() {
        return 1 + (int) (Math.random() * 250);
    }

    private AppUser loadAccount(String username) {
        return loadWithRoles(appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username).orElseThrow().getId());
    }

    private ResultMatcher notForbidden() {
        return result -> {
            int code = result.getResponse().getStatus();
            if (code == 403) {
                throw new AssertionError("Expected a status other than 403 for SUPER_ADMIN, but got 403");
            }
        };
    }

    @Test
    void superAdminOnlyCanAccessEveryAdminEndpoint() throws Exception {
        AppUser superAdmin = loadAccount("superadmin");
        AppUser admin = loadAccount("admin");
        AppUser finance = createUser("parityfin", Set.of(RoleCode.FINANCE_MANAGER));

        mockMvc.perform(post("/api/v1/parties/cleanup-phone")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/parties/cleanup-phone")
                        .header("Authorization", "Bearer " + mintAccessToken(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/parties/cleanup-phone")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());

        for (String endpoint : List.of("/api/v1/employees", "/api/v1/categories", "/api/v1/parties", "/api/v1/operations/items")) {
            mockMvc.perform(post(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                    .andExpect(notForbidden());
        }

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/employees/does-not-exist")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/categories/does-not-exist")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminOnlyCanCreateAndReviewAttendanceReports() throws Exception {
        AppUser superAdmin = loadAccount("superadmin");
        AppUser finance = createUser("rptparityfin", Set.of(RoleCode.FINANCE_MANAGER));

        mockMvc.perform(get("/api/v1/reports/preview")
                        .param("periodStart", "2026-07-01")
                        .param("periodEnd", "2026-07-31")
                        .param("payCycle", "MONTHLY")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(notForbidden());

        mockMvc.perform(get("/api/v1/reports/does-not-exist/decision-history")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(notForbidden());

        mockMvc.perform(post("/api/v1/reports/does-not-exist/approve")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/reports/does-not-exist/approve")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports/does-not-exist/reopen")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/reports/does-not-exist/downtime-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-07-01\",\"decision\":\"OK\"}")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(notForbidden());
    }

    @Test
    void superAdminOnlyCanUploadAndManageBiometricImports() throws Exception {
        AppUser superAdmin = loadAccount("superadmin");
        AppUser finance = createUser("impparityfin", Set.of(RoleCode.FINANCE_MANAGER));
        byte[] csv = ("Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
                + "EMP-101,2026-07-24,08:00,16:00,08:07,16:15\n").getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "attendance.csv", "text/csv", csv);

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("sourceId", "missing-source")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("sourceId", "missing-source")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/v1/imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(notForbidden());
        mockMvc.perform(multipart("/api/v1/imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/imports/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Parity Source\",\"sourceType\":\"FILE_DEVICE\",\"active\":true}")
                        .header("Authorization", "Bearer " + mintAccessToken(superAdmin)))
                .andExpect(status().isCreated());
    }

    @Test
    void unrelatedDomainRolesRemainForbidden() throws Exception {
        AppUser finance = createUser("domfin", Set.of(RoleCode.FINANCE_MANAGER));
        AppUser workforce = createUser("domwf", Set.of(RoleCode.WORKFORCE_MANAGER));
        AppUser hrManager = createUser("domhr", Set.of(RoleCode.HR_MANAGER));
        AppUser reviewer = createUser("domreviewer", Set.of(RoleCode.HR_REVIEWER));
        byte[] csv = ("Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
                + "EMP-101,2026-07-24,08:00,16:00,08:07,16:15\n").getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "attendance.csv", "text/csv", csv);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/employees/does-not-exist")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("sourceId", "missing-source")
                        .header("Authorization", "Bearer " + mintAccessToken(workforce)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/parties/cleanup-phone")
                        .header("Authorization", "Bearer " + mintAccessToken(hrManager)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/reports/does-not-exist/approve")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/categories/does-not-exist")
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isForbidden());
    }

    @Test
    void payrollTransitionAuthorizationSeparatesPreparationFromApprovalAndPosting() throws Exception {
        AppUser hrReviewer = createUser("payreviewer", Set.of(RoleCode.HR_REVIEWER), true);
        String reviewerToken = mintAccessToken(hrReviewer);

        mockMvc.perform(post("/api/v1/payroll/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodYear\":2026,\"periodMonth\":8,\"targetStatus\":\"POSTED\"}")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isForbidden());

        AppUser payrollManager = createUser("paytransition", Set.of(RoleCode.PAYROLL_MANAGER), true);
        mockMvc.perform(post("/api/v1/payroll/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodYear\":2026,\"periodMonth\":8,\"targetStatus\":\"PAID\"}")
                        .header("Authorization", "Bearer " + mintAccessToken(payrollManager)))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentProposalExecutionCannotBeCalledByProcurementOrViewerRoles() throws Exception {
        AppUser procurement = createUser("propexecproc", Set.of(RoleCode.PROCUREMENT_MANAGER));
        AppUser viewer = createUser("propexecview", Set.of(RoleCode.VIEWER));
        AppUser finance = createUser("propexecfin", Set.of(RoleCode.FINANCE_MANAGER));
        String payload = "{\"operationId\":\"security-op\",\"paymentMethod\":\"BANK_TRANSFER\"}";

        mockMvc.perform(post("/api/v1/procurement/payment-proposals/missing/execute")
                        .contentType(MediaType.APPLICATION_JSON).content(payload)
                        .header("Authorization", "Bearer " + mintAccessToken(procurement)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/procurement/payment-proposals/missing/execute")
                        .contentType(MediaType.APPLICATION_JSON).content(payload)
                        .header("Authorization", "Bearer " + mintAccessToken(viewer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/procurement/payment-proposals/missing/execute")
                        .contentType(MediaType.APPLICATION_JSON).content(payload)
                        .header("Authorization", "Bearer " + mintAccessToken(finance)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_PROPOSAL_NOT_FOUND"));
    }

    @Test
    void financeReportAndCloseAuthorizationUsesExplicitFinanceRoles() throws Exception {
        AppUser accountant = createUser("finreportacct", Set.of(RoleCode.ACCOUNTANT));
        AppUser auditor = createUser("finreportaudit", Set.of(RoleCode.AUDITOR));
        AppUser viewer = createUser("finreportview", Set.of(RoleCode.VIEWER));
        AppUser financeManager = createUser("finreportmgr", Set.of(RoleCode.FINANCE_MANAGER));

        mockMvc.perform(get("/api/v1/finance/reports/balance-sheet")
                        .param("asOfDate", "2026-08-31")
                        .header("Authorization", "Bearer " + mintAccessToken(accountant)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/finance/reports/balance-sheet")
                        .param("asOfDate", "2026-08-31")
                        .header("Authorization", "Bearer " + mintAccessToken(viewer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/finance/reconciliation/subledger/periods/missing")
                        .header("Authorization", "Bearer " + mintAccessToken(auditor)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/finance/reconciliation/subledger/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodId\":\"missing\",\"subledgerType\":\"AR\"}")
                        .header("Authorization", "Bearer " + mintAccessToken(auditor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/finance/reconciliation/subledger/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodId\":\"missing\",\"subledgerType\":\"AR\"}")
                        .header("Authorization", "Bearer " + mintAccessToken(accountant)))
                .andExpect(notForbidden());

        mockMvc.perform(post("/api/v1/finance/period-close/execute/missing")
                        .header("Authorization", "Bearer " + mintAccessToken(accountant)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/finance/period-close/execute/missing")
                        .header("Authorization", "Bearer " + mintAccessToken(financeManager)))
                .andExpect(notForbidden());
    }

    @Test
    void salesDeliveryAndReturnMutationsRequireSalesRole() throws Exception {
        AppUser viewer = createUser("o2cviewer", Set.of(RoleCode.VIEWER));
        AppUser finance = createUser("o2cfinance", Set.of(RoleCode.FINANCE_MANAGER));
        AppUser sales = createUser("o2csales", Set.of(RoleCode.SALES_MANAGER));
        String delivery = "{\"deliveryNumber\":\"D-1\",\"deliveryDate\":1,\"operationId\":\"d-op\"}";
        String returned = "{\"returnNumber\":\"R-1\",\"deliveryId\":\"missing\",\"returnDate\":1,\"reason\":\"test\",\"operationId\":\"r-op\",\"lines\":[{\"deliveryLineId\":\"missing\",\"quantity\":1,\"disposition\":\"AVAILABLE\"}]}";
        for (AppUser denied : List.of(viewer, finance)) {
            mockMvc.perform(post("/api/v1/trade/sales/orders/missing/deliveries").contentType(MediaType.APPLICATION_JSON)
                            .content(delivery).header("Authorization", "Bearer " + mintAccessToken(denied)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/trade/sales/orders/missing/returns").contentType(MediaType.APPLICATION_JSON)
                            .content(returned).header("Authorization", "Bearer " + mintAccessToken(denied)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/v1/trade/sales/orders/missing/deliveries").contentType(MediaType.APPLICATION_JSON)
                        .content(delivery).header("Authorization", "Bearer " + mintAccessToken(sales)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SALE_ORDER_NOT_FOUND"));
    }

    @Test
    void createRejectsInvalidMenuRoleCombination() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new AuthApi.UserUpsertRequest("badmenu-" + suffix, "Bad Menu", "Auth#Test1!",
                Set.of(RoleCode.VIEWER), Set.of("employees"), true, null, true, true, null, null);
        TenantContext.set(appId);

        assertThatThrownBy(() -> authService.create(request, "admin"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("ACCESS_MENU_ROLE_MISMATCH"));
    }

    @Test
    void updateRejectsInvalidMenuRoleCombination() {
        AppUser viewer = createUser("updmenu", Set.of(RoleCode.VIEWER));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new AuthApi.UserUpsertRequest("updmenu-" + suffix, "Bad Menu", null,
                Set.of(RoleCode.VIEWER), Set.of("employees"), true, null, true, true,
                viewer.getVersion(), null);

        assertThatThrownBy(() -> authService.update(viewer.getId(), request, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("ACCESS_MENU_ROLE_MISMATCH"));
    }

    @Test
    void createCarriesAcknowledgmentReasonIntoAudit() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new AuthApi.UserUpsertRequest("ackreason-" + suffix, "Ack Reason", "Auth#Test1!",
                Set.of(RoleCode.FINANCE_MANAGER), null, true, null, true, true, null, "created-for-finance");
        TenantContext.set(appId);

        var created = authService.create(request, "admin");
        createdUserIds.add(created.id());

        String details = auditLogRepository.findByEntityTypeOrderByOccurredAtDesc("USER",
                        PageRequest.of(0, 50)).getContent().stream()
                .filter(log -> log.getEntityId() != null && log.getEntityId().equals(created.id()))
                .filter(log -> "USER_CREATE".equals(log.getAction()))
                .findFirst()
                .map(com.bemo.hr.audit.domain.AuditLog::getDetailsJson)
                .orElseThrow();
        assertThat(details).contains("accessChangeReason=created-for-finance");
    }

    @Test
    void updateCarriesAcknowledgmentReasonIntoAudit() {
        AppUser finance = createUser("updack", Set.of(RoleCode.FINANCE_MANAGER));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var request = new AuthApi.UserUpsertRequest("updack-" + suffix, "Ack Reason", null,
                Set.of(RoleCode.FINANCE_MANAGER), null, true, null, true, true,
                finance.getVersion(), "audited-2026");

        authService.update(finance.getId(), request, "admin");

        String details = auditLogRepository.findByEntityTypeOrderByOccurredAtDesc("USER",
                        PageRequest.of(0, 50)).getContent().stream()
                .filter(log -> log.getEntityId() != null && log.getEntityId().equals(finance.getId()))
                .filter(log -> "USER_UPDATE".equals(log.getAction()))
                .findFirst()
                .map(com.bemo.hr.audit.domain.AuditLog::getDetailsJson)
                .orElseThrow();
        assertThat(details).contains("accessChangeReason=audited-2026");
    }
}
