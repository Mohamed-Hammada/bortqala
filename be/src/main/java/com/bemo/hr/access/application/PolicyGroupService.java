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
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.Role;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PolicyGroupService {

    private final SecurityPermissionRepository permissionRepository;
    private final SecurityPolicyGroupRepository policyGroupRepository;
    private final PolicyGroupPermissionRepository groupPermissionRepository;
    private final UserPolicyAssignmentRepository userPolicyAssignmentRepository;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;
    private final com.bemo.hr.access.domain.AccessCatalog accessCatalog;

    public PolicyGroupService(SecurityPermissionRepository permissionRepository,
                              SecurityPolicyGroupRepository policyGroupRepository,
                              PolicyGroupPermissionRepository groupPermissionRepository,
                              UserPolicyAssignmentRepository userPolicyAssignmentRepository,
                              AppUserRepository appUserRepository,
                              AuditService auditService) {
        this(permissionRepository, policyGroupRepository, groupPermissionRepository,
                userPolicyAssignmentRepository, appUserRepository, auditService, new com.bemo.hr.access.domain.AccessCatalog());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PolicyGroupService(SecurityPermissionRepository permissionRepository,
                              SecurityPolicyGroupRepository policyGroupRepository,
                              PolicyGroupPermissionRepository groupPermissionRepository,
                              UserPolicyAssignmentRepository userPolicyAssignmentRepository,
                              AppUserRepository appUserRepository,
                              AuditService auditService,
                              @org.springframework.lang.Nullable com.bemo.hr.access.domain.AccessCatalog accessCatalog) {
        this.permissionRepository = permissionRepository;
        this.policyGroupRepository = policyGroupRepository;
        this.groupPermissionRepository = groupPermissionRepository;
        this.userPolicyAssignmentRepository = userPolicyAssignmentRepository;
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
        this.accessCatalog = accessCatalog != null ? accessCatalog : new com.bemo.hr.access.domain.AccessCatalog();
    }

    public PolicyCatalogResponse getCatalog() {
        log.debug("getCatalog called");
        List<SecurityPermission> permissions = permissionRepository.findAllByOrderByModuleAscSubmoduleAscActionAsc();
        Map<String, List<PermissionDto>> byModule = new LinkedHashMap<>();
        for (SecurityPermission p : permissions) {
            byModule.computeIfAbsent(p.getModule(), k -> new ArrayList<>())
                    .add(new PermissionDto(
                            p.getId(),
                            p.getPermissionKey(),
                            p.getModule(),
                            p.getSubmodule(),
                            p.getDescriptionKey(),
                            p.getAction(),
                            p.isSystem()
                    ));
        }
        List<ModulePermissionTreeDto> moduleTrees = byModule.entrySet().stream()
                .map(e -> new ModulePermissionTreeDto(e.getKey(), e.getValue()))
                .toList();
        return new PolicyCatalogResponse(moduleTrees, permissions.size());
    }

    public List<PolicyGroupSummaryDto> listPolicyGroups() {
        String appId = TenantContext.require();
        List<SecurityPolicyGroup> groups = policyGroupRepository.findAllByAppIdOrderByGroupNameAsc(appId);
        List<PolicyGroupSummaryDto> result = new ArrayList<>();
        for (SecurityPolicyGroup g : groups) {
            int permCount = groupPermissionRepository.findByAppIdAndPolicyGroupId(appId, g.getId()).size();
            int userCount = (int) userPolicyAssignmentRepository.countByAppIdAndPolicyGroupId(appId, g.getId());
            result.add(new PolicyGroupSummaryDto(
                    g.getId(),
                    g.getGroupName(),
                    g.getDescription(),
                    g.isSystem(),
                    permCount,
                    userCount,
                    g.getCreatedAt(),
                    g.getUpdatedAt(),
                    g.getVersion()
            ));
        }
        return result;
    }

    public PolicyGroupDetailDto getPolicyGroup(String groupId) {
        String appId = TenantContext.require();
        SecurityPolicyGroup group = requireGroup(appId, groupId);
        List<PolicyGroupPermission> links = groupPermissionRepository.findByAppIdAndPolicyGroupId(appId, groupId);
        List<String> permIds = links.stream().map(PolicyGroupPermission::getPermissionId).toList();
        List<String> permKeys = permissionRepository.findByIdIn(permIds).stream()
                .map(SecurityPermission::getPermissionKey)
                .sorted()
                .toList();
        int userCount = (int) userPolicyAssignmentRepository.countByAppIdAndPolicyGroupId(appId, groupId);
        return new PolicyGroupDetailDto(
                group.getId(),
                group.getGroupName(),
                group.getDescription(),
                group.isSystem(),
                permKeys,
                userCount,
                group.getCreatedAt(),
                group.getUpdatedAt(),
                group.getVersion()
        );
    }

    @Transactional
    public PolicyGroupDetailDto createPolicyGroup(CreatePolicyGroupRequest request, String actorUsername) {
        log.debug("createPolicyGroup called with groupName={}", request.groupName());
        String appId = TenantContext.require();
        String groupName = request.groupName().strip();
        if (policyGroupRepository.existsByAppIdAndGroupNameIgnoreCase(appId, groupName)) {
            throw new BusinessRuleException("A policy group with this name already exists.",
                    "POLICY_GROUP_NAME_DUPLICATE", HttpStatus.CONFLICT);
        }

        SecurityPolicyGroup group = new SecurityPolicyGroup(groupName, request.description(), false);
        SecurityPolicyGroup saved = policyGroupRepository.save(group);

        List<String> savedKeys = assignPermissionsToGroup(appId, saved.getId(), request.permissionKeys());

        auditService.record("POLICY_GROUP_CREATE", "SECURITY_POLICY_GROUP", saved.getId(), actorUsername,
                "Created policy group: " + saved.getGroupName() + " with " + savedKeys.size() + " permissions", null);

        return new PolicyGroupDetailDto(
                saved.getId(),
                saved.getGroupName(),
                saved.getDescription(),
                saved.isSystem(),
                savedKeys,
                0,
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getVersion()
        );
    }

    @Transactional
    public PolicyGroupDetailDto updatePolicyGroup(String groupId, UpdatePolicyGroupRequest request, String actorUsername) {
        log.debug("updatePolicyGroup called with groupId={}", groupId);
        String appId = TenantContext.require();
        SecurityPolicyGroup group = requireGroup(appId, groupId);

        if (group.isSystem()) {
            throw new BusinessRuleException("System policy groups cannot be modified.",
                    "POLICY_GROUP_SYSTEM_IMMUTABLE", HttpStatus.FORBIDDEN);
        }

        String newName = request.groupName().strip();
        if (!group.getGroupName().equalsIgnoreCase(newName) &&
                policyGroupRepository.existsByAppIdAndGroupNameIgnoreCase(appId, newName)) {
            throw new BusinessRuleException("A policy group with this name already exists.",
                    "POLICY_GROUP_NAME_DUPLICATE", HttpStatus.CONFLICT);
        }

        group.update(newName, request.description());
        SecurityPolicyGroup saved = policyGroupRepository.save(group);

        groupPermissionRepository.deleteByAppIdAndPolicyGroupId(appId, groupId);
        List<String> savedKeys = assignPermissionsToGroup(appId, saved.getId(), request.permissionKeys());

        int userCount = (int) userPolicyAssignmentRepository.countByAppIdAndPolicyGroupId(appId, groupId);

        auditService.record("POLICY_GROUP_UPDATE", "SECURITY_POLICY_GROUP", saved.getId(), actorUsername,
                "Updated policy group: " + saved.getGroupName() + " with " + savedKeys.size() + " permissions", null);

        return new PolicyGroupDetailDto(
                saved.getId(),
                saved.getGroupName(),
                saved.getDescription(),
                saved.isSystem(),
                savedKeys,
                userCount,
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getVersion()
        );
    }

    @Transactional
    public void deletePolicyGroup(String groupId, String actorUsername) {
        log.debug("deletePolicyGroup called with groupId={}", groupId);
        String appId = TenantContext.require();
        SecurityPolicyGroup group = requireGroup(appId, groupId);

        if (group.isSystem()) {
            throw new BusinessRuleException("System policy groups cannot be deleted.",
                    "POLICY_GROUP_SYSTEM_IMMUTABLE", HttpStatus.FORBIDDEN);
        }

        groupPermissionRepository.deleteByAppIdAndPolicyGroupId(appId, groupId);
        userPolicyAssignmentRepository.deleteByAppIdAndPolicyGroupId(appId, groupId);
        policyGroupRepository.delete(group);

        auditService.record("POLICY_GROUP_DELETE", "SECURITY_POLICY_GROUP", groupId, actorUsername,
                "Deleted policy group: " + group.getGroupName(), null);
    }

    public List<UserPolicyAssignmentDto> getUserPolicyAssignments(String userId) {
        String appId = TenantContext.require();
        requireUser(appId, userId);
        List<UserPolicyAssignment> assignments = userPolicyAssignmentRepository.findByAppIdAndUserId(appId, userId);
        Map<String, String> groupNames = policyGroupRepository.findAllByAppIdOrderByGroupNameAsc(appId).stream()
                .collect(Collectors.toMap(SecurityPolicyGroup::getId, SecurityPolicyGroup::getGroupName));

        return assignments.stream().map(a -> new UserPolicyAssignmentDto(
                a.getId(),
                a.getUserId(),
                a.getPolicyGroupId(),
                groupNames.getOrDefault(a.getPolicyGroupId(), "Unknown Group"),
                a.getScopeBranchId(),
                a.getScopeCostCenterId(),
                a.getAssignedAt()
        )).toList();
    }

    public Map<String, List<UserPolicyAssignmentDto>> getUserPolicyAssignmentsForUsers(String appId, Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserPolicyAssignment> assignments = userPolicyAssignmentRepository.findByAppIdAndUserIdIn(appId, userIds);
        Map<String, String> groupNames = policyGroupRepository.findAllByAppIdOrderByGroupNameAsc(appId).stream()
                .collect(Collectors.toMap(SecurityPolicyGroup::getId, SecurityPolicyGroup::getGroupName));

        Map<String, List<UserPolicyAssignmentDto>> result = new HashMap<>();
        for (UserPolicyAssignment a : assignments) {
            result.computeIfAbsent(a.getUserId(), k -> new ArrayList<>())
                    .add(new UserPolicyAssignmentDto(
                            a.getId(),
                            a.getUserId(),
                            a.getPolicyGroupId(),
                            groupNames.getOrDefault(a.getPolicyGroupId(), "Unknown Group"),
                            a.getScopeBranchId(),
                            a.getScopeCostCenterId(),
                            a.getAssignedAt()
                    ));
        }
        return result;
    }

    @Transactional
    public List<UserPolicyAssignmentDto> assignUserPolicies(String userId, AssignUserPoliciesRequest request, String actorUsername) {
        log.debug("assignUserPolicies called for userId={}", userId);
        String appId = TenantContext.require();
        AppUser user = requireUser(appId, userId);

        userPolicyAssignmentRepository.deleteByAppIdAndUserId(appId, userId);

        List<UserPolicyAssignment> toSave = new ArrayList<>();
        if (request.assignments() != null) {
            for (UserPolicyAssignmentItem item : request.assignments()) {
                requireGroup(appId, item.policyGroupId());
                toSave.add(new UserPolicyAssignment(
                        userId,
                        item.policyGroupId(),
                        item.scopeBranchId(),
                        item.scopeCostCenterId()
                ));
            }
        }

        List<UserPolicyAssignment> saved = userPolicyAssignmentRepository.saveAll(toSave);

        auditService.record("USER_POLICY_ASSIGNMENT", "APP_USER", userId, actorUsername,
                "Assigned " + saved.size() + " policy groups to user " + user.getUsername(), null);

        return getUserPolicyAssignments(userId);
    }

    public UserEffectivePermissionsResponse getEffectivePermissions(String username) {
        String appId = TenantContext.require();
        AppUser user = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username, "AUTH_USER_NOT_FOUND"));

        boolean isAdmin = user.getRoles().stream().anyMatch(r ->
                r.getCode() == RoleCode.SUPER_ADMIN || r.getCode() == RoleCode.ADMIN);

        if (isAdmin) {
            List<SecurityPermission> allPerms = permissionRepository.findAll();
            Set<String> allKeys = allPerms.stream().map(SecurityPermission::getPermissionKey).collect(Collectors.toSet());
            allKeys.add("*");
            return new UserEffectivePermissionsResponse(
                    user.getId(),
                    user.getUsername(),
                    true,
                    allKeys,
                    Set.of("*"),
                    Set.of("*")
            );
        }

        List<UserPolicyAssignment> assignments = userPolicyAssignmentRepository.findByAppIdAndUserId(appId, user.getId());
        Set<String> groupIds = assignments.stream().map(UserPolicyAssignment::getPolicyGroupId).collect(Collectors.toSet());
        Set<String> branchScopes = assignments.stream()
                .map(UserPolicyAssignment::getScopeBranchId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> costCenterScopes = assignments.stream()
                .map(UserPolicyAssignment::getScopeCostCenterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<PolicyGroupPermission> links = groupPermissionRepository.findByPolicyGroupIdIn(groupIds);
        List<String> permIds = links.stream().map(PolicyGroupPermission::getPermissionId).toList();
        Set<String> permKeys = new HashSet<>(permissionRepository.findByIdIn(permIds).stream()
                .map(SecurityPermission::getPermissionKey)
                .collect(Collectors.toSet()));

        Set<RoleCode> userRoleCodes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        permKeys.addAll(getPermissionsForLegacyRoles(userRoleCodes));

        return new UserEffectivePermissionsResponse(
                user.getId(),
                user.getUsername(),
                false,
                permKeys,
                branchScopes,
                costCenterScopes
        );
    }

    private Set<String> getPermissionsForLegacyRoles(Set<RoleCode> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        Set<String> perms = new HashSet<>();
        for (RoleCode role : roleCodes) {
            if (role == RoleCode.SUPER_ADMIN || role == RoleCode.ADMIN) {
                perms.add("*");
            }
            if (accessCatalog != null) {
                perms.addAll(accessCatalog.permissionsOf(role.name()));
            }
        }

        Set<String> prefixes = new HashSet<>();
        for (RoleCode role : roleCodes) {
            switch (role) {
                case HR_MANAGER -> prefixes.addAll(List.of("hr", "payroll", "attendance", "leave", "workers", "reports", "biometric", "imports", "employees"));
                case HR_REVIEWER -> prefixes.addAll(List.of("attendance", "reports", "workers.read", "employees.read"));
                case PAYROLL_MANAGER -> prefixes.addAll(List.of("payroll", "salary"));
                case FINANCE_MANAGER, ACCOUNTANT -> prefixes.addAll(List.of("finance", "treasury", "budget", "compliance", "journal", "payments", "ledger"));
                case TREASURY_USER -> prefixes.addAll(List.of("treasury", "finance.journal", "journal.read"));
                case PROCUREMENT_MANAGER -> prefixes.addAll(List.of("procurement", "inventory", "orders", "suppliers"));
                case PROCUREMENT_USER -> prefixes.addAll(List.of("procurement", "inventory.stock"));
                case SALES_MANAGER -> prefixes.addAll(List.of("trade", "sales", "pos", "crm"));
                case INVENTORY_MANAGER -> prefixes.addAll(List.of("inventory", "operations", "stock"));
                case MANUFACTURING_MANAGER -> prefixes.addAll(List.of("manufacturing", "inventory", "bom", "production"));
                case QUALITY_MANAGER -> prefixes.addAll(List.of("quality", "manufacturing.quality"));
                case WORKFORCE_MANAGER -> prefixes.addAll(List.of("workforce", "contractor", "worker", "labor", "settlement", "advance", "timesheet"));
                case WORKFORCE_REVIEWER -> prefixes.addAll(List.of("settlement.read", "settlement.prepare", "timesheet", "attendance.review"));
                case WORKFORCE_FINANCE -> prefixes.addAll(List.of("settlement", "contractorAccounts", "finance"));
                case PROJECT_MANAGER -> prefixes.addAll(List.of("contracting", "project", "procurement", "wbs", "claims"));
                case AUDITOR -> prefixes.addAll(List.of("audit", "finance.read", "compliance"));
                case VIEWER -> prefixes.addAll(List.of("dashboard.view", "reports.read", "settings.read", "projects.read"));
                default -> {}
            }
        }

        if (!prefixes.isEmpty()) {
            permissionRepository.findAll().stream()
                    .map(SecurityPermission::getPermissionKey)
                    .filter(key -> prefixes.stream().anyMatch(pfx -> key.startsWith(pfx) || key.startsWith(pfx + ":") || key.startsWith(pfx + ".")))
                    .forEach(perms::add);
        }

        return perms;
    }

    private List<String> assignPermissionsToGroup(String appId, String groupId, List<String> permissionKeys) {
        if (permissionKeys == null || permissionKeys.isEmpty()) {
            return List.of();
        }
        List<SecurityPermission> perms = permissionRepository.findByPermissionKeyIn(permissionKeys);
        List<PolicyGroupPermission> links = perms.stream()
                .map(p -> new PolicyGroupPermission(groupId, p.getId()))
                .toList();
        groupPermissionRepository.saveAll(links);
        return perms.stream().map(SecurityPermission::getPermissionKey).sorted().toList();
    }

    private SecurityPolicyGroup requireGroup(String appId, String id) {
        return policyGroupRepository.findByIdAndAppId(id, appId)
                .orElseThrow(() -> new NotFoundException("Policy group not found: " + id, "POLICY_GROUP_NOT_FOUND"));
    }

    private AppUser requireUser(String appId, String id) {
        return appUserRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id, "AUTH_USER_NOT_FOUND"));
    }
}
