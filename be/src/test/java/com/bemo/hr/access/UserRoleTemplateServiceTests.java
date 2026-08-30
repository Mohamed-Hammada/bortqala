package com.bemo.hr.access;

import com.bemo.hr.access.application.UserRoleTemplateService;
import com.bemo.hr.access.domain.AccessCatalog;
import com.bemo.hr.access.domain.PolicyGroupPermission;
import com.bemo.hr.access.domain.SecurityPermission;
import com.bemo.hr.access.domain.SecurityPolicyGroup;
import com.bemo.hr.access.domain.UserRoleTemplate;
import com.bemo.hr.access.domain.UserRoleTemplateRepository;
import com.bemo.hr.access.infrastructure.PolicyGroupPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPolicyGroupRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeatureService;
import com.bemo.hr.tenant.api.TenantSetupApi.TenantVerticalResponse;
import com.bemo.hr.tenant.application.TenantSetupService;
import com.bemo.hr.tenant.domain.BusinessVertical;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** WP-10: menu catalog + vertical role template resolution. */
class UserRoleTemplateServiceTests {

    private AccessCatalog accessCatalog;
    private UserRoleTemplateRepository templateRepository;
    private SecurityPolicyGroupRepository policyGroupRepository;
    private PolicyGroupPermissionRepository groupPermissionRepository;
    private SecurityPermissionRepository permissionRepository;
    private TenantSetupService tenantSetupService;
    private TenantFeatureService featureService;
    private UserRoleTemplateService service;

    @BeforeEach
    void setUp() {
        accessCatalog = new AccessCatalog();
        templateRepository = mock(UserRoleTemplateRepository.class);
        policyGroupRepository = mock(SecurityPolicyGroupRepository.class);
        groupPermissionRepository = mock(PolicyGroupPermissionRepository.class);
        permissionRepository = mock(SecurityPermissionRepository.class);
        tenantSetupService = mock(TenantSetupService.class);
        featureService = mock(TenantFeatureService.class);

        service = new UserRoleTemplateService(accessCatalog, templateRepository,
                policyGroupRepository, groupPermissionRepository, permissionRepository,
                tenantSetupService, featureService);

        TenantContext.set("app-1");
        lenient().when(tenantSetupService.getVerticalSetup()).thenReturn(new TenantVerticalResponse(
                "app-1", BusinessVertical.MEDICAL, Set.of("finance.enabled"), List.of()));
        lenient().when(templateRepository.findForTenant(org.mockito.ArgumentMatchers.anyString(),
                anyCollection())).thenReturn(List.of());
        lenient().when(policyGroupRepository.findAllByAppIdOrderByGroupNameAsc("app-1"))
                .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private UserRoleTemplate global(String code, String vertical, String menus, String prefixes, int sort) {
        return new UserRoleTemplate(null, vertical, code, "users.template." + code.toLowerCase(),
                menus, prefixes, sort);
    }

    @Test
    void menuOptionsMarkFeatureGatedMenusDisabledAndTagVerticals() {
        when(featureService.isEnabled("app-1", "manufacturing.enabled")).thenReturn(false);
        when(featureService.isEnabled("app-1", "sales.enabled")).thenReturn(true);

        var options = service.menuOptions();

        var production = options.stream()
                .filter(option -> option.id().equals("production"))
                .findFirst().orElseThrow();
        assertFalse(production.enabled());
        assertTrue(production.verticalTags().contains("MANUFACTURING"));
        assertFalse(production.verticalTags().contains("MEDICAL"));

        var dashboard = options.stream()
                .filter(option -> option.id().equals("dashboard"))
                .findFirst().orElseThrow();
        assertTrue(dashboard.enabled());
        assertEquals(6, dashboard.verticalTags().size());

        var sales = options.stream()
                .filter(option -> option.id().equals("sales"))
                .findFirst().orElseThrow();
        assertTrue(sales.enabled());
        assertTrue(sales.verticalTags().contains("MEDICAL"));
    }

    @Test
    void templatesMergeGeneralWithTenantVerticalAndDedupeByCode() {
        UserRoleTemplate doctor = global("DOCTOR", "MEDICAL", "employees,reports",
                "hr:employee", 10);
        UserRoleTemplate accountant = global("ACCOUNTANT", "GENERAL", "accounts,journal-entries",
                "finance:journal", 30);
        UserRoleTemplate tenantDoctor = new UserRoleTemplate("app-1", "MEDICAL", "DOCTOR",
                "users.template.customDoctor", "employees", "hr:employee:read", 5);
        when(templateRepository.findForTenant("app-1", Set.of("MEDICAL", "GENERAL")))
                .thenReturn(List.of(doctor, accountant, tenantDoctor));

        List<com.bemo.hr.access.api.AccessTemplateApi.RoleTemplateResponse> templates =
                service.roleTemplates(null);

        assertEquals(2, templates.size());
        var effectiveDoctor = templates.stream()
                .filter(template -> template.code().equals("DOCTOR"))
                .findFirst().orElseThrow();
        assertEquals("users.template.customDoctor", effectiveDoctor.nameKey());
        assertEquals(List.of("employees"), effectiveDoctor.menuIds());
        assertTrue(templates.stream().anyMatch(template -> template.code().equals("ACCOUNTANT")));
    }

    @Test
    void explicitVerticalParamIsUsedAndGeneralAlwaysAdded() {
        when(templateRepository.findForTenant(org.mockito.ArgumentMatchers.eq("app-1"),
                org.mockito.ArgumentMatchers.eq(Set.of("RETAIL", "GENERAL"))))
                .thenReturn(List.of(global("CASHIER", "RETAIL", "pos", "pos:checkout", 10)));

        var templates = service.roleTemplates("retail");

        assertEquals(1, templates.size());
        assertEquals("CASHIER", templates.get(0).code());
    }

    @Test
    void unknownVerticalIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.roleTemplates("GAMING"));
    }

    @Test
    void suggestedGroupsAreRankedByPrefixOverlap() {
        SecurityPolicyGroup pharmacy = new SecurityPolicyGroup("Medical Receptionist", "front desk", true);
        SecurityPolicyGroup finance = new SecurityPolicyGroup("Financial Controller", "gl", true);
        when(policyGroupRepository.findAllByAppIdOrderByGroupNameAsc("app-1"))
                .thenReturn(List.of(pharmacy, finance));

        SecurityPermission soRead = new SecurityPermission("sales:so:read", "trade", "so",
                "perm.x", "READ", true);
        SecurityPermission journalRead = new SecurityPermission("finance:journal:read", "finance",
                "journal", "perm.y", "READ", true);
        when(permissionRepository.findByIdIn(anyCollection()))
                .thenReturn(List.of(soRead, journalRead));

        PolicyGroupPermission pharmacyGrant = new PolicyGroupPermission(pharmacy.getId(), soRead.getId());
        PolicyGroupPermission financeGrant = new PolicyGroupPermission(finance.getId(), journalRead.getId());
        when(groupPermissionRepository.findByPolicyGroupIdIn(anyCollection()))
                .thenReturn(List.of(pharmacyGrant, financeGrant));

        when(templateRepository.findForTenant("app-1", Set.of("MEDICAL", "GENERAL")))
                .thenReturn(List.of(global("PHARMACIST", "MEDICAL", "sales,pos,reports",
                        "sales:so,pos:", 20)));

        var templates = service.roleTemplates(null);

        assertEquals(1, templates.size());
        assertEquals(List.of(pharmacy.getId()), templates.get(0).suggestedPolicyGroupIds());
        assertNotNull(templates.get(0).nameKey());
    }

    @Test
    void verticalTagsCoverAllVerticalsForUngatedFeature() {
        assertTrue(UserRoleTemplateService.verticalTagsFor(null).size() == 6);
        assertTrue(UserRoleTemplateService.verticalTagsFor("unknown.feature").size() == 6);
        assertEquals(List.of("CIVIL", "GENERAL"),
                UserRoleTemplateService.verticalTagsFor("workforce.contractorAccounts.enabled"));
    }
}
