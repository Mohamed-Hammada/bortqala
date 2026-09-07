package com.bemo.hr.party.api;

import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.AuthApi;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.shared.security.JwtProperties;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
 * JSON) proof that {@code GET /api/v1/parties/reports/aging?asOfDate=X} genuinely excludes
 * transactions that occurred after the requested cutoff, rather than merely accepting the
 * parameter. No prior test in the suite exercised this endpoint over real HTTP at all —
 * {@code PartyFinancialPositionServiceTests} covers the same asOfDate logic, but with a mocked
 * repository, not a real persisted, tenant-scoped H2 database reached through the real
 * {@code @PreAuthorize} chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PartyFinancialPositionAsOfDateIntegrationTests {

    private final AuthService authService;
    private final AppUserRepository appUserRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final MockMvc mockMvc;
    private final TransactionTemplate tx;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdPartyIds = new ArrayList<>();
    private final List<String> createdLedgerEntryIds = new ArrayList<>();
    private String appId;
    private String appCode;

    @Autowired
    PartyFinancialPositionAsOfDateIntegrationTests(AuthService authService,
                                                    AppUserRepository appUserRepository,
                                                    TenantApplicationRepository tenantApplicationRepository,
                                                    BusinessPartyRepository businessPartyRepository,
                                                    PartnerLedgerEntryRepository partnerLedgerEntryRepository,
                                                    JwtEncoder jwtEncoder,
                                                    JwtProperties jwtProperties,
                                                    MockMvc mockMvc,
                                                    PlatformTransactionManager transactionManager) {
        this.authService = authService;
        this.appUserRepository = appUserRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.businessPartyRepository = businessPartyRepository;
        this.partnerLedgerEntryRepository = partnerLedgerEntryRepository;
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
            partnerLedgerEntryRepository.deleteAllById(createdLedgerEntryIds);
            businessPartyRepository.deleteAllById(createdPartyIds);
            appUserRepository.deleteAllById(createdUserIds);
        } finally {
            createdUserIds.clear();
            createdPartyIds.clear();
            createdLedgerEntryIds.clear();
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
        return tx.execute(status -> {
            AppUser user = appUserRepository.findById(created.id()).orElseThrow();
            user.getRoles().size();
            return user;
        });
    }

    private String mintAccessToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
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
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    @Test
    void agingReportAsOfDateExcludesTransactionsThatOccurredAfterTheCutoff() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        BusinessParty party = tx.execute(status -> {
            BusinessParty p = new BusinessParty("ASOF-" + suffix, "As-Of Date Test Customer " + suffix, null, "CUSTOMER",
                    null, null, null, null, null, true,
                    "DIRECT", null, "2026-01-01", null, "EGP", "STANDARD", "NET_30", null, null);
            p.updateCreditProfile(BigDecimal.valueOf(1_000_000), false, 30);
            return businessPartyRepository.save(p);
        });
        createdPartyIds.add(party.getId());

        Instant pastOccurred = Instant.now().minus(Duration.ofDays(60));
        Instant afterCutoffOccurred = Instant.now().minus(Duration.ofDays(2));
        Instant cutoff = Instant.now().minus(Duration.ofDays(10));

        // A real invoice-type ledger entry well before the cutoff — must always be included.
        PartnerLedgerEntry beforeCutoffEntry = tx.execute(status -> partnerLedgerEntryRepository.save(
                new PartnerLedgerEntry(party.getId(), "INVOICE", BigDecimal.valueOf(50_000), "INV-ASOF-1",
                        "Before cutoff", pastOccurred, "system")));
        createdLedgerEntryIds.add(beforeCutoffEntry.getId());

        // A real invoice-type ledger entry that occurred AFTER the historical cutoff but before
        // real "now" — a genuinely future transaction relative to the as-of date being queried.
        PartnerLedgerEntry afterCutoffEntry = tx.execute(status -> partnerLedgerEntryRepository.save(
                new PartnerLedgerEntry(party.getId(), "INVOICE", BigDecimal.valueOf(777_777), "INV-ASOF-2",
                        "After cutoff", afterCutoffOccurred, "system")));
        createdLedgerEntryIds.add(afterCutoffEntry.getId());

        AppUser financeManager = createUser("asoffin", Set.of(RoleCode.FINANCE_MANAGER));
        String token = mintAccessToken(financeManager);

        // --- Historical view (asOfDate = cutoff, before the second entry): only the first entry counts. ---
        mockMvc.perform(get("/api/v1/parties/reports/aging")
                        .param("partyType", "CUSTOMER")
                        .param("asOfDate", String.valueOf(cutoff.toEpochMilli()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[?(@.partyId=='" + party.getId() + "')].totalBalance")
                        .value(org.hamcrest.Matchers.contains(50000.0)));

        // --- Current view (no asOfDate): both entries count, proving the historical exclusion above
        // was genuinely date-driven and not simply "this party's data is always filtered out". ---
        mockMvc.perform(get("/api/v1/parties/reports/aging")
                        .param("partyType", "CUSTOMER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[?(@.partyId=='" + party.getId() + "')].totalBalance")
                        .value(org.hamcrest.Matchers.contains(827777.0)));
    }
}
