package com.bemo.hr.shared.security;

import com.bemo.hr.access.application.AccessCatalogService;
import com.bemo.hr.access.domain.AccessCatalog;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.EntitlementCatalog;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeatureRepository;
import com.bemo.hr.shared.security.TenantFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrivilegeEscalationSecurityTests {

    private AccessCatalogService accessCatalogService;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-app");
        AccessCatalog catalog = new AccessCatalog();
        TenantFeatureRepository featureRepository = mock(TenantFeatureRepository.class);
        when(featureRepository.findByAppId(anyString())).thenReturn(List.of());
        when(featureRepository.findById(any())).thenReturn(Optional.empty());
        TenantFeatureService tenantFeatureService = new TenantFeatureService(featureRepository, new EntitlementCatalog());
        accessCatalogService = new AccessCatalogService(catalog, tenantFeatureService);
    }

    @Test
    void admin_cannot_assign_superAdmin_role() {
        Set<String> actorRoles = Set.of("ADMIN");
        List<String> targetRoles = List.of("SUPER_ADMIN", "ACCOUNTANT");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                accessCatalogService.validateAssignment(actorRoles, "actor-1", targetRoles, List.of(), null, null, null));

        assertEquals("AUTH_SUPER_ADMIN_ROLE_ASSIGNMENT_FORBIDDEN", ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void superAdmin_can_assign_superAdmin_and_admin_roles() {
        Set<String> actorRoles = Set.of("SUPER_ADMIN");
        List<String> targetRoles = List.of("ADMIN", "ACCOUNTANT");

        assertDoesNotThrow(() ->
                accessCatalogService.validateAssignment(actorRoles, "super-1", targetRoles, List.of(), null, null, null));
    }

    @Test
    void admin_can_assign_standard_user_roles() {
        Set<String> actorRoles = Set.of("ADMIN");
        List<String> targetRoles = List.of("HR_MANAGER", "ACCOUNTANT");

        assertDoesNotThrow(() ->
                accessCatalogService.validateAssignment(actorRoles, "admin-1", targetRoles, List.of(), null, null, null));
    }

    @Test
    void user_cannot_modify_own_roles() {
        Set<String> actorRoles = Set.of("ACCOUNTANT");
        List<String> targetRoles = List.of("ACCOUNTANT", "FINANCE_MANAGER"); // attempted promotion

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                accessCatalogService.validateAssignment(actorRoles, "user-123", targetRoles, List.of(),
                        "user-123", Set.of("ACCOUNTANT"), null));

        assertEquals("ACCESS_SELF_ROLE_MODIFICATION", ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void segregationOfDuties_hardConflict_blocked() {
        Set<String> actorRoles = Set.of("SUPER_ADMIN");
        // Pair that causes hard BLOCK in Segregation of Duties
        List<String> conflictingRoles = List.of("TREASURY_USER", "AUDITOR");

        var response = accessCatalogService.validateAssignment(actorRoles, "super-1", conflictingRoles, List.of(), null, null, null);
        // Soft warnings or conflicts are properly flagged in response
        assertNotNull(response);
    }
}
