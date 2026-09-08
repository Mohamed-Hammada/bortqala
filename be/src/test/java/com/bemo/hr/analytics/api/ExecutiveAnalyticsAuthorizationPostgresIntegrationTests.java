package com.bemo.hr.analytics.api;

import com.bemo.hr.PostgresIntegrationTest;
import com.bemo.hr.analytics.domain.ExecutiveCockpitTarget;
import com.bemo.hr.analytics.domain.ExecutiveKpiSnapshot;
import com.bemo.hr.analytics.infrastructure.ExecutiveCockpitTargetRepository;
import com.bemo.hr.analytics.infrastructure.ExecutiveKpiSnapshotRepository;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.JwtProperties;
import com.bemo.hr.shared.security.Role;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.security.RoleRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Real-PostgreSQL counterpart to the two persistence/concurrency tests in
 * {@link ExecutiveAnalyticsAuthorizationIntegrationTests} (snapshot upsert, first-time target
 * creation race). Those H2-backed tests remain in place unchanged — this class exists solely to
 * prove the same behavior against a real {@code postgres:17-alpine} container, per the same
 * pattern already proven by {@code PayrollPaymentConcurrencyTests},
 * {@code SupplierPaymentConcurrencyTests}, {@code VendorPaymentProposalConcurrencyTests}, and
 * {@code PunchSourceIdentityConcurrencyTests}: real HTTP through the real Spring Security filter
 * chain, a real two-thread race for the target-creation test, and a direct real-repository read
 * afterward — no mocked {@code DataIntegrityViolationException}, no simulated concurrency.
 * <p>
 * A brand-new {@link TenantApplication} is created per test (matching the four classes above)
 * rather than relying on the pre-seeded "TEST" tenant, so users are persisted directly via
 * {@link AppUserRepository} (mirroring {@code ExecutiveAnalyticsAuthorizationIntegrationTests
 * #bootstrapUser}) instead of {@code AuthService.create(...)}, which requires an already-existing
 * actor user in the target tenant.
 */
@AutoConfigureMockMvc
class ExecutiveAnalyticsAuthorizationPostgresIntegrationTests extends PostgresIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private TenantApplicationRepository tenantApplicationRepository;
    @Autowired
    private ExecutiveCockpitTargetRepository cockpitTargetRepository;
    @Autowired
    private ExecutiveKpiSnapshotRepository kpiSnapshotRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private String appId;
    private String appCode;

    @AfterEach
    void cleanup() {
        try {
            if (appId == null) return;
            TenantContext.set(appId);
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.executeWithoutResult(status -> {
                cockpitTargetRepository.findAll().forEach(t -> cockpitTargetRepository.deleteById(t.getId()));
                kpiSnapshotRepository.findAll().forEach(s -> kpiSnapshotRepository.deleteById(s.getId()));
                appUserRepository.findAll().forEach(u -> appUserRepository.deleteById(u.getId()));
            });
            tenantApplicationRepository.deleteById(appId);
        } finally {
            appId = null;
            TenantContext.clear();
        }
    }

    /**
     * Creates a fresh tenant and, within the same transaction, a real {@link Role} row (find-or-
     * create, mirroring the same idiom {@code AuthService} itself uses) and a real {@link AppUser}
     * for it — bypassing {@code AuthService.create(...)}, which requires an existing actor user
     * that a brand-new tenant does not have.
     */
    private AppUser bootstrapProjectManager(TransactionTemplate tx, String prefix) {
        return tx.execute(status -> {
            TenantApplication app = tenantApplicationRepository.save(
                    new TenantApplication("EXECPG" + UUID.randomUUID().toString().substring(0, 6), "Executive Analytics Postgres Test"));
            appId = app.getId();
            appCode = app.getCode();
            TenantContext.set(appId);

            if (!roleRepository.existsById(RoleCode.PROJECT_MANAGER)) {
                roleRepository.save(new Role(RoleCode.PROJECT_MANAGER, RoleCode.PROJECT_MANAGER.name().replace('_', ' ')));
            }
            Role role = roleRepository.findById(RoleCode.PROJECT_MANAGER).orElseThrow();

            String suffix = UUID.randomUUID().toString().substring(0, 8);
            AppUser user = new AppUser(appId, prefix + "-" + suffix, "Test " + prefix,
                    passwordEncoder.encode("Auth#Test1!"), Set.of(role), null, true, true);
            appUserRepository.save(user);
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

    @Test
    void recordingTheSameSnapshotTwiceUpsertsARealRowRatherThanDuplicatingItOnRealPostgres() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        AppUser projectManager = bootstrapProjectManager(tx, "execsnapdup");
        String token = mintAccessToken(projectManager, appCode);
        String kpiKey = "DUP_TEST_KPI_" + UUID.randomUUID().toString().substring(0, 8);
        String firstPayload = "{\"periodKey\":\"2026-Q9\",\"category\":\"FINANCIAL\",\"kpiKey\":\"" + kpiKey + "\",\"actualValue\":100}";
        String secondPayload = "{\"periodKey\":\"2026-Q9\",\"category\":\"FINANCIAL\",\"kpiKey\":\"" + kpiKey + "\",\"actualValue\":250}";

        mockMvc.perform(post("/api/v1/analytics/executive/snapshots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstPayload))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
        mockMvc.perform(post("/api/v1/analytics/executive/snapshots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondPayload))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        // Real persistence check against real PostgreSQL (not a Mockito interaction): exactly one
        // row for this period/category/kpiKey combination exists, carrying the LATEST value.
        // TenantContext must be set BEFORE calling tx.execute(...), not inside the lambda —
        // TransactionTemplate opens the Hibernate session (and resolves its @TenantId) as part of
        // beginning the transaction, before the callback body runs; the two real HTTP calls above
        // already cleared TenantContext on this thread (RequestAuditFilter clears it in a finally
        // block after every request).
        TenantContext.set(appId);
        List<ExecutiveKpiSnapshot> rows = tx.execute(status ->
                kpiSnapshotRepository.findByPeriodKeyOrderByCategoryAscKpiKeyAsc("2026-Q9").stream()
                        .filter(s -> s.getKpiKey().equals(kpiKey))
                        .toList());
        org.assertj.core.api.Assertions.assertThat(rows).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(rows.get(0).getActualValue())
                .isEqualByComparingTo(BigDecimal.valueOf(250));
    }

    @Test
    void concurrentFirstTimeTargetCreationForTheSamePeriodProducesOneSuccessAndOneCleanConflictOnRealPostgres() throws Exception {
        // Real concurrency, real persistence, real DB unique constraint (app_id, period_key) on
        // real PostgreSQL — not a mocked DataIntegrityViolationException. Two real HTTP requests
        // for a brand-new periodKey are released simultaneously via a CyclicBarrier;
        // ExecutiveAnalyticsService.saveTargets does a find-then-insert, so both requests can pass
        // the "not found" check before either commits, and the real unique constraint must reject
        // the loser.
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        AppUser user = bootstrapProjectManager(tx, "exectgtconc");
        String token = mintAccessToken(user, appCode);
        String periodKey = "C" + UUID.randomUUID().toString().substring(0, 12);
        String payload = "{\"periodKey\":\"" + periodKey + "\"}";

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Integer> attempt = () -> {
            barrier.await();
            return mockMvc.perform(post("/api/v1/analytics/executive/targets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andReturn().getResponse().getStatus();
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(attempt);
            Future<Integer> second = pool.submit(attempt);
            int statusA = first.get(15, TimeUnit.SECONDS);
            int statusB = second.get(15, TimeUnit.SECONDS);

            // Real persistence proof against real PostgreSQL: regardless of which request "won",
            // exactly one target row exists for this period afterward.
            TenantContext.set(appId);
            List<ExecutiveCockpitTarget> rows = tx.execute(status ->
                    cockpitTargetRepository.findAll().stream()
                            .filter(t -> t.getPeriodKey().equals(periodKey))
                            .toList());
            org.assertj.core.api.Assertions.assertThat(rows).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(List.of(statusA, statusB))
                    .as("one request creates (200), the other hits the real unique-constraint conflict (409); status=%s/%s",
                            statusA, statusB)
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
    }
}
