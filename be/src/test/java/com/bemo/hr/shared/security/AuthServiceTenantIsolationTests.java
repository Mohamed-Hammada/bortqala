package com.bemo.hr.shared.security;

import com.bemo.hr.shared.domain.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthServiceTenantIsolationTests {

    private final AuthService authService;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final TenantApplicationRepository tenantApplicationRepository;

    private final List<String> createdAppIds = new ArrayList<>();
    private final List<String> createdUserIds = new ArrayList<>();

    @Autowired
    AuthServiceTenantIsolationTests(AuthService authService,
                                    AppUserRepository appUserRepository,
                                    RoleRepository roleRepository,
                                    TenantApplicationRepository tenantApplicationRepository) {
        this.authService = authService;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
    }

    @AfterEach
    void cleanup() {
        try {
            appUserRepository.deleteAllById(createdUserIds);
            tenantApplicationRepository.deleteAllById(createdAppIds);
        } finally {
            createdUserIds.clear();
            createdAppIds.clear();
            TenantContext.clear();
        }
    }

    @Test
    void mustNotReturnUserFromAnotherTenant() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantApplication tenantA = tenantApplicationRepository.save(
                new TenantApplication("TENA-" + suffix, "Tenant A"));
        TenantApplication tenantB = tenantApplicationRepository.save(
                new TenantApplication("TENB-" + suffix, "Tenant B"));
        createdAppIds.add(tenantA.getId());
        createdAppIds.add(tenantB.getId());

        TenantContext.set(tenantB.getId());
        var reviewerRole = roleRepository.findById(RoleCode.VIEWER).orElseThrow();
        AppUser reviewer = new AppUser(tenantB.getId(), "reviewer", "Reviewer", "hash", Set.of(reviewerRole),
                null, null, null);
        appUserRepository.save(reviewer);
        createdUserIds.add(reviewer.getId());

        TenantContext.set(tenantA.getId());
        assertThatThrownBy(() -> authService.current("reviewer"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found.");
    }
}
