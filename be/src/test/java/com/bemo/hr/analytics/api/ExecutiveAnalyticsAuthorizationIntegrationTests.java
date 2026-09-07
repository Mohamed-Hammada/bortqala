package com.bemo.hr.analytics.api;

import com.bemo.hr.analytics.domain.ExecutiveCockpitTarget;
import com.bemo.hr.analytics.infrastructure.ExecutiveCockpitTargetRepository;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.AuthApi;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.shared.security.JwtProperties;
import com.bemo.hr.shared.security.Role;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.security.RoleRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real Spring-Security-executing integration tests for {@link ExecutiveAnalyticsController}.
 * <p>
 * docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, High Finding H-4 / Security Finding #2: the previous
 * {@code ExecutiveAnalyticsControllerTests} were pure Mockito unit tests against a plain
 * {@code new ExecutiveAnalyticsController(mockService)} — no {@code @PreAuthorize} annotation on
 * any of the 8 endpoints was ever exercised by any test. This class mints real JWTs, sends real
 * HTTP requests through the real Spring Security filter chain via {@link MockMvc}, and proves:
 * an allowed role succeeds, a disallowed authenticated role gets 403, an unauthenticated caller
 * gets 401, and a different tenant's saved data is never visible (real {@code @TenantId} filter).
 * <p>
 * <b>Known, documented gap</b> (not fixed in this remediation pass): a true HTTP-level
 * "branch access denied" proof for {@code GET /cockpit?branchId=...} would require standing up a
 * restrictive {@code PolicyGroup} with a non-empty, non-matching {@code branchScopes} set for a
 * non-admin test user — no such fixture exists anywhere in the test suite today (confirmed by
 * repository-wide search), and building one is test infrastructure, not a remediation fix. The
 * {@code BRANCH_ACCESS_DENIED} logic itself IS regression-tested at the service layer (see
 * {@code ExecutiveAnalyticsServiceTests#branchAccessDenialIsEnforced}, which mocks
 * {@code SecurityAuthorizationEvaluator} directly and proves the 403 is thrown), and is honestly
 * reported as a real-HTTP test gap in the final remediation report rather than left unstated.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExecutiveAnalyticsAuthorizationIntegrationTests {

    private final AuthService authService;
    private final AppUserRepository appUserRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final ExecutiveCockpitTargetRepository cockpitTargetRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final MockMvc mockMvc;
    private final TransactionTemplate tx;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdTenantIds = new ArrayList<>();
    private String appId;
    private String appCode;

    @Autowired
    ExecutiveAnalyticsAuthorizationIntegrationTests(AuthService authService,
                                                    AppUserRepository appUserRepository,
                                                    TenantApplicationRepository tenantApplicationRepository,
                                                    ExecutiveCockpitTargetRepository cockpitTargetRepository,
                                                    RoleRepository roleRepository,
                                                    PasswordEncoder passwordEncoder,
                                                    JwtEncoder jwtEncoder,
                                                    JwtProperties jwtProperties,
                                                    MockMvc mockMvc,
                                                    PlatformTransactionManager transactionManager) {
        this.authService = authService;
        this.appUserRepository = appUserRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.cockpitTargetRepository = cockpitTargetRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.mockMvc = mockMvc;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void setUp() {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow();
        appId = app.getId();
        appCode = app.getCode();
        TenantContext.set(appId);
    }

    @AfterEach
    void cleanup() {
        try {
            appUserRepository.deleteAllById(createdUserIds);
            for (String tenantId : createdTenantIds) {
                TenantContext.set(tenantId);
                cockpitTargetRepository.findAll().forEach(t -> cockpitTargetRepository.deleteById(t.getId()));
            }
            tenantApplicationRepository.deleteAllById(createdTenantIds);
        } finally {
            createdUserIds.clear();
            createdTenantIds.clear();
            TenantContext.clear();
        }
    }

    private AppUser createUser(String prefix, Set<RoleCode> roles, String forAppId) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + "-" + suffix;
        var request = new AuthApi.UserUpsertRequest(username, "Test " + prefix, "Auth#Test1!",
                roles, null, true, null, true, true, null, null);
        TenantContext.set(forAppId);
        var created = authService.create(request, "admin");
        createdUserIds.add(created.id());
        return loadWithRoles(created.id());
    }

    /**
     * Directly persists a user for a brand-new tenant that has no existing "admin" actor yet.
     * {@link AuthService#create} requires an existing actor user to authorize the assignment
     * (see {@code AccessCatalogService.validateAssignment}) — irrelevant for what this test proves
     * (tenant-scoped data isolation of {@code ExecutiveCockpitTarget}), so this bypasses that
     * self-service-creation validation the same way a tenant-bootstrap flow would.
     */
    private AppUser bootstrapUser(String prefix, RoleCode role, String forAppId) {
        return tx.execute(status -> {
            TenantContext.set(forAppId);
            Role roleEntity = roleRepository.findById(role).orElseThrow();
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            AppUser user = new AppUser(forAppId, prefix + "-" + suffix, "Test " + prefix,
                    passwordEncoder.encode("Auth#Test1!"), Set.of(roleEntity), null, true, true);
            appUserRepository.save(user);
            createdUserIds.add(user.getId());
            user.getRoles().size();
            return user;
        });
    }

    private AppUser loadWithRoles(String userId) {
        return tx.execute(status -> {
            AppUser user = appUserRepository.findById(userId).orElseThrow();
            user.getRoles().size();
            return user;
        });
    }

    private String mintAccessToken(AppUser user, String forAppCode) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(30)))
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("appId", user.getAppId())
                .claim("appCode", forAppCode)
                .claim("name", user.getDisplayName())
                .claim("tv", user.getTokenVersion())
                .claim("pwc", false)
                .claim("roles", user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList());
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    // ---- Allowed-role matrix: SUPER_ADMIN, ADMIN, FINANCE_MANAGER, PROJECT_MANAGER, GENERAL_MANAGER ----

    @ParameterizedTest(name = "{0} {1} allows FINANCE_MANAGER")
    @CsvSource({
            "GET,/api/v1/analytics/executive/kpi-registry",
            "GET,/api/v1/analytics/executive/overview",
            "GET,/api/v1/analytics/executive/trends",
            "GET,/api/v1/analytics/executive/snapshots",
            "GET,/api/v1/analytics/executive/cockpit",
            "GET,/api/v1/analytics/executive/cockpit/export.xlsx",
            "GET,/api/v1/analytics/executive/targets",
    })
    void allowedRoleSucceedsOnGetEndpoints(String method, String path) throws Exception {
        AppUser finance = createUser("execfin", Set.of(RoleCode.FINANCE_MANAGER), appId);
        mockMvc.perform(get(path).header("Authorization", "Bearer " + mintAccessToken(finance, appCode)))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest(name = "{0} {1} rejects HR_REVIEWER (outside the 5-role matrix)")
    @CsvSource({
            "GET,/api/v1/analytics/executive/kpi-registry",
            "GET,/api/v1/analytics/executive/overview",
            "GET,/api/v1/analytics/executive/trends",
            "GET,/api/v1/analytics/executive/snapshots",
            "GET,/api/v1/analytics/executive/cockpit",
            "GET,/api/v1/analytics/executive/cockpit/export.xlsx",
            "GET,/api/v1/analytics/executive/targets",
    })
    void disallowedRoleIsRejectedOnGetEndpoints(String method, String path) throws Exception {
        AppUser reviewer = createUser("execrev", Set.of(RoleCode.HR_REVIEWER), appId);
        mockMvc.perform(get(path).header("Authorization", "Bearer " + mintAccessToken(reviewer, appCode)))
                .andExpect(status().isForbidden());
    }

    @Test
    void recordSnapshotAllowsProjectManagerAndRejectsHrReviewer() throws Exception {
        AppUser projectManager = createUser("execsnappm", Set.of(RoleCode.PROJECT_MANAGER), appId);
        AppUser reviewer = createUser("execsnaprev", Set.of(RoleCode.HR_REVIEWER), appId);
        String payload = "{\"periodKey\":\"2026-Q9\",\"category\":\"FINANCIAL\",\"kpiKey\":\"AUTH_TEST_KPI\",\"actualValue\":100}";

        mockMvc.perform(post("/api/v1/analytics/executive/snapshots")
                        .header("Authorization", "Bearer " + mintAccessToken(projectManager, appCode))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/analytics/executive/snapshots")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer, appCode))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveTargetsAllowsProjectManagerAndRejectsHrReviewer() throws Exception {
        AppUser projectManager = createUser("exectgtpm", Set.of(RoleCode.PROJECT_MANAGER), appId);
        AppUser reviewer = createUser("exectgtrev", Set.of(RoleCode.HR_REVIEWER), appId);
        // period_key column is VARCHAR(20) — keep the generated key within that limit.
        String periodKey = "T" + UUID.randomUUID().toString().substring(0, 12);
        String payload = "{\"periodKey\":\"" + periodKey + "\"}";

        mockMvc.perform(post("/api/v1/analytics/executive/targets")
                        .header("Authorization", "Bearer " + mintAccessToken(projectManager, appCode))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/analytics/executive/targets")
                        .header("Authorization", "Bearer " + mintAccessToken(reviewer, appCode))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCallerIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/executive/cockpit"))
                .andExpect(status().isUnauthorized());
    }

    // Note: an ADMIN/SUPER_ADMIN-role variant of the above was deliberately not added — AuthService's
    // own self-service user-creation path (AccessCatalogService.validateAssignment) requires the
    // *acting* user to already be a real, persisted SUPER_ADMIN to create an ADMIN account, which
    // this test harness's synthetic "admin" actor string does not satisfy. That is a pre-existing,
    // orthogonal constraint in user self-service creation, not part of this remediation's scope.
    // FINANCE_MANAGER/PROJECT_MANAGER above already exercise the same generic hasAnyRole(...) gate
    // that SUPER_ADMIN/ADMIN also pass through.

    // ---- Tenant isolation: a saved target in tenant A must never be visible to tenant B ----

    @Test
    void savedTargetIsNotVisibleToADifferentTenant() throws Exception {
        // period_key column is VARCHAR(20) — keep the generated key within that limit.
        String periodKey = "T" + UUID.randomUUID().toString().substring(0, 12);

        // Tenant A: save a real, distinctive target.
        AppUser tenantAUser = createUser("exectenA", Set.of(RoleCode.FINANCE_MANAGER), appId);
        String tenantAToken = mintAccessToken(tenantAUser, appCode);
        String payload = "{\"periodKey\":\"" + periodKey + "\",\"targetRevenue\":9999999}";
        mockMvc.perform(post("/api/v1/analytics/executive/targets")
                        .header("Authorization", "Bearer " + tenantAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Tenant B: a separate, freshly-created tenant application.
        String tenantBId = tx.execute(status -> {
            TenantApplication tenantB = new TenantApplication("TESTISO" + UUID.randomUUID().toString().substring(0, 6), "Isolation Test Tenant");
            return tenantApplicationRepository.save(tenantB).getId();
        });
        createdTenantIds.add(tenantBId);
        TenantApplication tenantBApp = tenantApplicationRepository.findById(tenantBId).orElseThrow();
        AppUser tenantBUser = bootstrapUser("exectenB", RoleCode.FINANCE_MANAGER, tenantBId);
        String tenantBToken = mintAccessToken(tenantBUser, tenantBApp.getCode());

        // The same periodKey under tenant B must return the system default, NOT tenant A's 9,999,999.
        mockMvc.perform(get("/api/v1/analytics/executive/targets")
                        .param("periodKey", periodKey)
                        .header("Authorization", "Bearer " + tenantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("default"))
                .andExpect(jsonPath("$.targetRevenue").value(1500000.00));

        TenantContext.set(appId);
    }
}
