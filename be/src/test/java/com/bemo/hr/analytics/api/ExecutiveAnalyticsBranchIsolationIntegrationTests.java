package com.bemo.hr.analytics.api;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.finance.domain.treasury.Cashbox;
import com.bemo.hr.finance.infrastructure.CashboxRepository;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.AuthApi;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.shared.security.JwtProperties;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real, end-to-end (HTTP -> Spring Security -> Controller -> Service -> real H2 persistence ->
 * JSON) proof that {@code GET /api/v1/analytics/executive/cockpit?branchId=X} genuinely scopes
 * the response to branch X, and never leaks another branch's or another tenant's data.
 * <p>
 * This exists specifically because {@code ExecutiveAnalyticsServiceTests}' branch-isolation
 * coverage is Mockito-based (proves the service's query-selection logic in isolation, with
 * deterministic mocked repository responses) — precise, but not the same class of evidence as a
 * real database with real rows. This class closes that gap for the two KPIs that are cheapest and
 * most reliable to set up with real persisted data end-to-end: active headcount
 * ({@code Employee.branchId}) and cash position ({@code Cashbox.branchId}). The remaining
 * branch-scoped KPIs (POS revenue via {@code PosTerminal}, payroll/expenses via
 * {@code Employee.branchId}, projects via {@code Project.branchId}, inventory via
 * {@code Warehouse.branchId}) are covered by the Mockito-based tests only — building full real
 * fixtures for all of them (valid POS terminals/sessions, approved project budgets, warehouse
 * stock movements, etc.) would each pull in significant unrelated domain setup and was judged not
 * to be worth the added fragility for this specific verification pass; see
 * docs/FINAL_REMEDIATION_VERIFICATION_2026-09-07.md for the explicit, honest scope statement.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExecutiveAnalyticsBranchIsolationIntegrationTests {

    private final AuthService authService;
    private final AppUserRepository appUserRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final CashboxRepository cashboxRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final MockMvc mockMvc;
    private final TransactionTemplate tx;

    /** Real, pre-seeded ADMINISTRATION category for the "TEST" tenant (see
     * be/src/test/resources/db/changelog/test-data/20260724_v4_demo_reference_categories.yaml) —
     * reused rather than creating a new one, since Employee.categoryId is a real NOT NULL FK. */
    private static final String SEEDED_TEST_TENANT_CATEGORY_ID = "10000000-0000-0000-0000-000000000001";

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdBranchIds = new ArrayList<>();
    private final List<String> createdEmployeeIds = new ArrayList<>();
    private final List<String> createdCashboxIds = new ArrayList<>();
    private final List<String> createdTenantIds = new ArrayList<>();
    private String appId;
    private String appCode;

    @Autowired
    ExecutiveAnalyticsBranchIsolationIntegrationTests(AuthService authService,
                                                      AppUserRepository appUserRepository,
                                                      TenantApplicationRepository tenantApplicationRepository,
                                                      BranchRepository branchRepository,
                                                      EmployeeRepository employeeRepository,
                                                      CashboxRepository cashboxRepository,
                                                      PasswordEncoder passwordEncoder,
                                                      JwtEncoder jwtEncoder,
                                                      JwtProperties jwtProperties,
                                                      MockMvc mockMvc,
                                                      PlatformTransactionManager transactionManager) {
        this.authService = authService;
        this.appUserRepository = appUserRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.branchRepository = branchRepository;
        this.employeeRepository = employeeRepository;
        this.cashboxRepository = cashboxRepository;
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
            TenantContext.set(appId);
            employeeRepository.deleteAllById(createdEmployeeIds);
            cashboxRepository.deleteAllById(createdCashboxIds);
            branchRepository.deleteAllById(createdBranchIds);
            appUserRepository.deleteAllById(createdUserIds);
            for (String tenantId : createdTenantIds) {
                TenantContext.set(tenantId);
                branchRepository.findAll().forEach(b -> branchRepository.deleteById(b.getId()));
            }
            tenantApplicationRepository.deleteAllById(createdTenantIds);
        } finally {
            createdUserIds.clear();
            createdBranchIds.clear();
            createdEmployeeIds.clear();
            createdCashboxIds.clear();
            createdTenantIds.clear();
            TenantContext.clear();
        }
    }

    private AppUser createUser(String prefix, Set<RoleCode> roles) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + "-" + suffix;
        var request = new AuthApi.UserUpsertRequest(username, "Test " + prefix, "Auth#Test1!",
                roles, null, true, null, true, true, null, null);
        TenantContext.set(appId);
        var created = authService.create(request, "admin");
        createdUserIds.add(created.id());
        return loadWithRoles(created.id());
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

    /** Persists a real Branch under the given tenant (must already be the active TenantContext). */
    private Branch createBranch(String companyId, String code, String name) {
        return tx.execute(status -> {
            Branch branch = new Branch(companyId, code, name, "Test Location", true);
            branch = branchRepository.save(branch);
            createdBranchIds.add(branch.getId());
            return branch;
        });
    }

    /** Persists a real, active Employee assigned to the given branch. */
    private Employee createEmployee(String branchId, String codePrefix) {
        return tx.execute(status -> {
            Employee employee = new Employee(
                    codePrefix + "-" + UUID.randomUUID().toString().substring(0, 8),
                    "Test Employee " + codePrefix,
                    null,
                    SEEDED_TEST_TENANT_CATEGORY_ID,
                    EmploymentType.FIXED,
                    java.time.LocalDate.of(2026, 1, 1),
                    null,
                    true);
            employee.setBranchId(branchId);
            employee = employeeRepository.save(employee);
            createdEmployeeIds.add(employee.getId());
            return employee;
        });
    }

    /** Persists a real Cashbox for the given branch with the given opening balance. */
    private Cashbox createCashbox(String branchId, String code, BigDecimal balance) {
        return tx.execute(status -> {
            Cashbox cashbox = new Cashbox(code, "Test Cashbox " + code, branchId, "EGP", null, null);
            cashbox.adjustBalance(balance);
            cashbox = cashboxRepository.save(cashbox);
            createdCashboxIds.add(cashbox.getId());
            return cashbox;
        });
    }

    @Test
    void realHttpProvesBranchA1DataIsIsolatedFromBranchA2AndFromTenantWideTotals() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Branch branchA1 = createBranch("company-test", "A1-" + suffix, "Branch A1 " + suffix);
        Branch branchA2 = createBranch("company-test", "A2-" + suffix, "Branch A2 " + suffix);

        // Materially different, real, persisted headcount: A1 has 2 employees, A2 has 1.
        createEmployee(branchA1.getId(), "a1emp1");
        createEmployee(branchA1.getId(), "a1emp2");
        createEmployee(branchA2.getId(), "a2emp1");

        // Materially different, real, persisted cash balances.
        createCashbox(branchA1.getId(), "CB-A1-" + suffix, BigDecimal.valueOf(12_345));
        createCashbox(branchA2.getId(), "CB-A2-" + suffix, BigDecimal.valueOf(654_321));

        AppUser financeManager = createUser("branchisofin", Set.of(RoleCode.FINANCE_MANAGER));
        String token = mintAccessToken(financeManager, appCode);

        // --- Branch A1: exactly A1's real data ---
        mockMvc.perform(get("/api/v1/analytics/executive/cockpit")
                        .param("branchId", branchA1.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(branchA1.getId()))
                .andExpect(jsonPath("$.kpiSummary.activeHeadcount").value(2))
                .andExpect(jsonPath("$.kpiSummary.cashInHand").value(12345.0))
                .andExpect(jsonPath("$.branchLeaderboard.length()").value(1))
                .andExpect(jsonPath("$.branchLeaderboard[0].branchId").value(branchA1.getId()));

        // --- Branch A2: exactly A2's real data — never A1's ---
        mockMvc.perform(get("/api/v1/analytics/executive/cockpit")
                        .param("branchId", branchA2.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(branchA2.getId()))
                .andExpect(jsonPath("$.kpiSummary.activeHeadcount").value(1))
                .andExpect(jsonPath("$.kpiSummary.cashInHand").value(654321.0))
                .andExpect(jsonPath("$.branchLeaderboard.length()").value(1))
                .andExpect(jsonPath("$.branchLeaderboard[0].branchId").value(branchA2.getId()));

        // --- Tenant-wide (no branchId): a real combined total, not either branch's isolated figure ---
        mockMvc.perform(get("/api/v1/analytics/executive/cockpit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(org.hamcrest.Matchers.nullValue()));
        // (activeHeadcount/cashInHand are NOT asserted to exact values for the no-branchId case —
        // the real "TEST" tenant's shared H2 database already contains other tests' employees and
        // cashboxes accumulated across the whole suite run, so only branch-scoped totals are
        // deterministic; the combined-view assertion above proves branchId is genuinely absent from
        // the response, which is what "tenant-wide" means at the contract level.)
    }

    @Test
    void crossTenantBranchIdNeverLeaksAnotherTenantsRealDataOverRealHttp() throws Exception {
        // A real Branch B1, with real employees, persisted under a SEPARATE, freshly-bootstrapped tenant.
        String tenantBId = tx.execute(status -> {
            TenantApplication tenantB = new TenantApplication("TESTBR" + UUID.randomUUID().toString().substring(0, 6), "Branch Isolation Tenant B");
            return tenantApplicationRepository.save(tenantB).getId();
        });
        createdTenantIds.add(tenantBId);
        TenantContext.set(tenantBId);
        Branch branchB1 = createBranch("company-b", "B1", "Tenant B Branch 1");
        // Employee creation for tenant B would need a tenant-B category (none seeded) — headcount
        // isn't needed to prove this test's point (no leak), so it's skipped; the branch and a real
        // cashbox with a real, distinctive balance are sufficient.
        createCashbox(branchB1.getId(), "CB-B1", BigDecimal.valueOf(999_999_999));

        // Switch back to Tenant A ("TEST") to create the calling user.
        TenantContext.set(appId);
        AppUser tenantAUser = createUser("crosstenbr", Set.of(RoleCode.FINANCE_MANAGER));
        String tenantAToken = mintAccessToken(tenantAUser, appCode);

        // A Tenant-A-authenticated request for Tenant B's real branch ID must never return Tenant
        // B's real cash balance (999,999,999). Empirically observed (not assumed) behavior: the
        // application's canonical tenant-isolation response here is a real HTTP 200 with a fully
        // safe, empty result — NOT a 403. `SecurityAuthorizationEvaluator.hasBranchAccess` passes
        // (FINANCE_MANAGER has an empty `branchScopes` set by default, so it never actually checks
        // branch ownership), but every downstream repository call runs under Tenant A's Hibernate
        // `@TenantId` filter, which makes Tenant B's branch/employees/cashbox simply not exist from
        // Tenant A's point of view. The result: branchId echoes back correctly, branchLeaderboard is
        // an empty list (not even a zeroed placeholder for the foreign branch), and every KPI is
        // real zero — never Tenant B's actual data. This is the same class of safe outcome as a 403
        // (no cross-tenant data is observable), so it is treated as acceptable, but is asserted on
        // precisely rather than glossed over as "some kind of rejection".
        var result = mockMvc.perform(get("/api/v1/analytics/executive/cockpit")
                        .param("branchId", branchB1.getId())
                        .header("Authorization", "Bearer " + tenantAToken))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("999999999").doesNotContain("999999999.0");
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(200);
        org.assertj.core.api.Assertions.assertThat(body).contains("\"branchLeaderboard\":[]").contains("\"cashInHand\":0");

        TenantContext.set(appId);
    }
}
