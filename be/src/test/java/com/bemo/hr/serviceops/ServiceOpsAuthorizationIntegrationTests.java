package com.bemo.hr.serviceops;

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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the {@code @PreAuthorize} matrix added to BookingController/RentalController/
 * WorkOrderController on 2026-09-06 actually rejects an unauthorized authenticated caller.
 * These endpoints previously carried no authorization annotation at all (see
 * docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md, Security Findings #2) — this test class would have
 * failed against that prior state, since {@code disallowedRole} would have received 200/201/404
 * instead of 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ServiceOpsAuthorizationIntegrationTests {

    private final AuthService authService;
    private final AppUserRepository appUserRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final MockMvc mockMvc;
    private final TransactionTemplate tx;

    private final List<String> createdUserIds = new ArrayList<>();
    private String appId;
    private String appCode;

    @Autowired
    ServiceOpsAuthorizationIntegrationTests(AuthService authService,
                                            AppUserRepository appUserRepository,
                                            TenantApplicationRepository tenantApplicationRepository,
                                            JwtEncoder jwtEncoder,
                                            JwtProperties jwtProperties,
                                            MockMvc mockMvc,
                                            PlatformTransactionManager transactionManager) {
        this.authService = authService;
        this.appUserRepository = appUserRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
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
        } finally {
            createdUserIds.clear();
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

    // ---- BookingController ----

    @Test
    void listBookingResourcesRejectsRoleOutsideTheClassLevelMatrix() throws Exception {
        AppUser viewer = createUser("boviewer", Set.of(RoleCode.VIEWER));

        // VIEWER is not in BookingController's class-level @PreAuthorize role list, so even a
        // read-only, unannotated GET method must still be rejected (class-level guard applies).
        mockMvc.perform(get("/api/v1/service-ops/bookings/resources")
                        .header("Authorization", "Bearer " + mintAccessToken(viewer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBookingResourceRequiresSalesOrInventoryOrAdminRole() throws Exception {
        AppUser sales = createUser("bosales", Set.of(RoleCode.SALES_MANAGER));
        AppUser hr = createUser("bohr", Set.of(RoleCode.HR_MANAGER));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String payloadJson = "{\"code\":\"ROOM-A-" + suffix + "\",\"name\":\"Room A\",\"kind\":\"ROOM\"}";

        mockMvc.perform(post("/api/v1/service-ops/bookings/resources")
                        .header("Authorization", "Bearer " + mintAccessToken(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/service-ops/bookings/resources")
                        .header("Authorization", "Bearer " + mintAccessToken(hr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson.replace("ROOM-A-" + suffix, "ROOM-B-" + suffix)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingEndpointsRejectUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/service-ops/bookings/resources"))
                .andExpect(status().isUnauthorized());
    }

    // ---- RentalController ----

    @Test
    void createRentalItemRequiresSalesOrInventoryOrAdminRole() throws Exception {
        AppUser inventory = createUser("rlinv", Set.of(RoleCode.INVENTORY_MANAGER));
        AppUser workforce = createUser("rlwf", Set.of(RoleCode.WORKFORCE_MANAGER));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String payloadJson = "{\"code\":\"RNT-1-" + suffix + "\",\"name\":\"Excavator\",\"category\":\"HEAVY_EQUIPMENT\",\"rateDaily\":500}";

        mockMvc.perform(post("/api/v1/service-ops/rentals/items")
                        .header("Authorization", "Bearer " + mintAccessToken(inventory))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/service-ops/rentals/items")
                        .header("Authorization", "Bearer " + mintAccessToken(workforce))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson.replace("RNT-1-" + suffix, "RNT-2-" + suffix)))
                .andExpect(status().isForbidden());
    }

    // ---- WorkOrderController ----

    @Test
    void deliverAndCreateInvoiceIsRestrictedTighterThanPlainWrites() throws Exception {
        AppUser inventory = createUser("wodeliv", Set.of(RoleCode.INVENTORY_MANAGER));

        // INVENTORY_MANAGER is in the class-level default, so plain listing succeeds...
        mockMvc.perform(get("/api/v1/service-ops/work-orders")
                        .header("Authorization", "Bearer " + mintAccessToken(inventory)))
                .andExpect(status().isOk());

        // ...but the invoice-generating delivery action has a tighter method-level override that
        // excludes INVENTORY_MANAGER specifically (see WorkOrderController.deliverAndCreateInvoice).
        mockMvc.perform(post("/api/v1/service-ops/work-orders/nonexistent-id/deliver")
                        .header("Authorization", "Bearer " + mintAccessToken(inventory)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deliverAndCreateInvoiceAllowsSalesManager() throws Exception {
        AppUser sales = createUser("wosales", Set.of(RoleCode.SALES_MANAGER));

        // Sales manager passes the @PreAuthorize gate; 404 (not 403) proves authorization succeeded
        // and the request reached the service layer, which then reports the work order as missing.
        mockMvc.perform(post("/api/v1/service-ops/work-orders/nonexistent-id/deliver")
                        .header("Authorization", "Bearer " + mintAccessToken(sales)))
                .andExpect(status().isNotFound());
    }
}
