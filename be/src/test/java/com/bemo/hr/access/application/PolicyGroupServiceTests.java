package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessPolicyApi.*;
import com.bemo.hr.access.domain.PolicyGroupPermission;
import com.bemo.hr.access.domain.SecurityPermission;
import com.bemo.hr.access.domain.SecurityPolicyGroup;
import com.bemo.hr.access.domain.UserPolicyAssignment;
import com.bemo.hr.access.infrastructure.PolicyGroupPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPolicyGroupRepository;
import com.bemo.hr.access.infrastructure.UserPolicyAssignmentRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.Role;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PolicyGroupServiceTests {

    private final SecurityPermissionRepository permissionRepository = mock(SecurityPermissionRepository.class);
    private final SecurityPolicyGroupRepository policyGroupRepository = mock(SecurityPolicyGroupRepository.class);
    private final PolicyGroupPermissionRepository groupPermissionRepository = mock(PolicyGroupPermissionRepository.class);
    private final UserPolicyAssignmentRepository userPolicyAssignmentRepository = mock(UserPolicyAssignmentRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private PolicyGroupService service;

    @BeforeEach
    void setUp() {
        TenantContext.set("app-123");
        service = new PolicyGroupService(
                permissionRepository,
                policyGroupRepository,
                groupPermissionRepository,
                userPolicyAssignmentRepository,
                appUserRepository,
                auditService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getCatalog_groupsPermissionsByModule() {
        SecurityPermission p1 = new SecurityPermission("sales:so:create", "trade", "orders", "desc.1", "create", true);
        SecurityPermission p2 = new SecurityPermission("finance:journal:post", "finance", "journals", "desc.2", "post", true);

        when(permissionRepository.findAllByOrderByModuleAscSubmoduleAscActionAsc()).thenReturn(List.of(p2, p1));

        PolicyCatalogResponse catalog = service.getCatalog();

        assertThat(catalog.totalPermissions()).isEqualTo(2);
        assertThat(catalog.modules()).hasSize(2);
    }

    @Test
    void createPolicyGroup_savesGroupAndPermissions() {
        CreatePolicyGroupRequest req = new CreatePolicyGroupRequest(
                "Site Accountant",
                "Custom accounting group",
                List.of("finance:journal:post", "finance:journal:read")
        );

        when(policyGroupRepository.existsByAppIdAndGroupNameIgnoreCase("app-123", "Site Accountant")).thenReturn(false);
        when(policyGroupRepository.save(any(SecurityPolicyGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        SecurityPermission p1 = new SecurityPermission("finance:journal:post", "finance", "journals", "d1", "post", true);
        SecurityPermission p2 = new SecurityPermission("finance:journal:read", "finance", "journals", "d2", "read", true);
        when(permissionRepository.findByPermissionKeyIn(req.permissionKeys())).thenReturn(List.of(p1, p2));

        PolicyGroupDetailDto result = service.createPolicyGroup(req, "admin");

        assertThat(result.groupName()).isEqualTo("Site Accountant");
        assertThat(result.permissionKeys()).containsExactlyInAnyOrder("finance:journal:post", "finance:journal:read");
        verify(groupPermissionRepository).saveAll(any());
        verify(auditService).record(eq("POLICY_GROUP_CREATE"), any(), any(), eq("admin"), any(), any());
    }

    @Test
    void createPolicyGroup_duplicateName_throwsConflict() {
        CreatePolicyGroupRequest req = new CreatePolicyGroupRequest("Store Keeper", "desc", List.of());
        when(policyGroupRepository.existsByAppIdAndGroupNameIgnoreCase("app-123", "Store Keeper")).thenReturn(true);

        assertThatThrownBy(() -> service.createPolicyGroup(req, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updatePolicyGroup_systemGroup_throwsForbidden() {
        SecurityPolicyGroup systemGroup = new SecurityPolicyGroup("System Admin", "desc", true);
        when(policyGroupRepository.findByIdAndAppId("grp-1", "app-123")).thenReturn(Optional.of(systemGroup));

        UpdatePolicyGroupRequest req = new UpdatePolicyGroupRequest("System Admin Edited", "desc", List.of(), 0L);

        assertThatThrownBy(() -> service.updatePolicyGroup("grp-1", req, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be modified");
    }

    @Test
    void deletePolicyGroup_systemGroup_throwsForbidden() {
        SecurityPolicyGroup systemGroup = new SecurityPolicyGroup("System Admin", "desc", true);
        when(policyGroupRepository.findByIdAndAppId("grp-1", "app-123")).thenReturn(Optional.of(systemGroup));

        assertThatThrownBy(() -> service.deletePolicyGroup("grp-1", "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be deleted");
    }

    @Test
    void assignUserPolicies_replacesUserAssignments() {
        AppUser user = new AppUser("app-123", "john", "John Doe", "hash",
                Set.of(new Role(RoleCode.ACCOUNTANT, "Accountant")), Set.of(), true, true);
        when(appUserRepository.findByAppIdAndId("app-123", "usr-1")).thenReturn(Optional.of(user));

        SecurityPolicyGroup grp1 = new SecurityPolicyGroup("Site Accountant", "desc", false);
        when(policyGroupRepository.findByIdAndAppId("grp-1", "app-123")).thenReturn(Optional.of(grp1));
        when(policyGroupRepository.findAllByAppIdOrderByGroupNameAsc("app-123")).thenReturn(List.of(grp1));

        AssignUserPoliciesRequest req = new AssignUserPoliciesRequest(List.of(
                new UserPolicyAssignmentItem("grp-1", "branch-cairo", "cc-project-1")
        ));

        when(userPolicyAssignmentRepository.findByAppIdAndUserId("app-123", "usr-1")).thenReturn(List.of(
                new UserPolicyAssignment("usr-1", "grp-1", "branch-cairo", "cc-project-1")
        ));

        List<UserPolicyAssignmentDto> result = service.assignUserPolicies("usr-1", req, "admin");

        verify(userPolicyAssignmentRepository).deleteByAppIdAndUserId("app-123", "usr-1");
        verify(userPolicyAssignmentRepository).saveAll(any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).scopeBranchId()).isEqualTo("branch-cairo");
    }

    @Test
    void getEffectivePermissions_adminUser_returnsWildcard() {
        AppUser admin = new AppUser("app-123", "admin", "Admin User", "hash",
                Set.of(new Role(RoleCode.ADMIN, "Admin")), Set.of(), true, true);
        when(appUserRepository.findByAppIdAndUsernameIgnoreCase("app-123", "admin")).thenReturn(Optional.of(admin));

        SecurityPermission p1 = new SecurityPermission("sales:so:create", "trade", "orders", "d1", "create", true);
        when(permissionRepository.findAll()).thenReturn(List.of(p1));

        UserEffectivePermissionsResponse effective = service.getEffectivePermissions("admin");

        assertThat(effective.isAdmin()).isTrue();
        assertThat(effective.permissions()).contains("*", "sales:so:create");
    }

    @Test
    void getEffectivePermissions_customUser_resolvesPoliciesAndScopes() {
        AppUser user = new AppUser("app-123", "site_eng", "Site Engineer", "hash",
                Set.of(new Role(RoleCode.VIEWER, "Viewer")), Set.of(), true, true);
        when(appUserRepository.findByAppIdAndUsernameIgnoreCase("app-123", "site_eng")).thenReturn(Optional.of(user));

        UserPolicyAssignment assign = new UserPolicyAssignment(user.getId(), "grp-1", "branch-alex", "cc-100");
        when(userPolicyAssignmentRepository.findByAppIdAndUserId("app-123", user.getId())).thenReturn(List.of(assign));

        PolicyGroupPermission pgp = new PolicyGroupPermission("grp-1", "perm-1");
        when(groupPermissionRepository.findByPolicyGroupIdIn(Set.of("grp-1"))).thenReturn(List.of(pgp));

        SecurityPermission p = new SecurityPermission("contracting:dpr:approve", "contracting", "dpr", "d1", "approve", true);
        when(permissionRepository.findByIdIn(List.of("perm-1"))).thenReturn(List.of(p));
        when(permissionRepository.findAll()).thenReturn(List.of());

        UserEffectivePermissionsResponse effective = service.getEffectivePermissions("site_eng");

        assertThat(effective.isAdmin()).isFalse();
        assertThat(effective.permissions()).contains("contracting:dpr:approve");
        assertThat(effective.branchScopes()).containsExactly("branch-alex");
        assertThat(effective.costCenterScopes()).containsExactly("cc-100");
    }
}
