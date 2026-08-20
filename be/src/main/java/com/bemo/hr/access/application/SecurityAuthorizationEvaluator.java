package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessPolicyApi.UserEffectivePermissionsResponse;
import com.bemo.hr.shared.security.RoleCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * Custom SpEL authorization evaluator bean registered as {@code @auth}.
 * <p>
 * Supports fine-grained method security annotations:
 * <ul>
 *     <li>{@code @PreAuthorize("@auth.hasPermission('finance:journal:post')")}</li>
 *     <li>{@code @PreAuthorize("@auth.hasAnyPermission('sales:so:create', 'sales:so:update')")}</li>
 *     <li>{@code @PreAuthorize("@auth.hasBranchAccess(#branchId)")}</li>
 * </ul>
 */
@Slf4j
@Component("auth")
public class SecurityAuthorizationEvaluator {

    private final PolicyGroupService policyGroupService;
    private final EffectivePermissionCache permissionCache;

    public SecurityAuthorizationEvaluator(PolicyGroupService policyGroupService) {
        this(policyGroupService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SecurityAuthorizationEvaluator(PolicyGroupService policyGroupService,
                                          @org.springframework.lang.Nullable EffectivePermissionCache permissionCache) {
        this.policyGroupService = policyGroupService;
        this.permissionCache = permissionCache;
    }

    public boolean hasPermission(String permissionKey) {
        if (permissionKey == null || permissionKey.isBlank()) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        if (isAdmin(auth)) {
            return true;
        }

        UserEffectivePermissionsResponse effective = getEffective(auth.getName());
        if (effective == null) {
            return false;
        }

        Set<String> perms = effective.permissions();
        return perms.contains("*") || perms.contains(permissionKey.strip());
    }

    public boolean hasAnyPermission(String... permissionKeys) {
        if (permissionKeys == null || permissionKeys.length == 0) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        if (isAdmin(auth)) {
            return true;
        }

        UserEffectivePermissionsResponse effective = getEffective(auth.getName());
        if (effective == null) {
            return false;
        }

        Set<String> perms = effective.permissions();
        if (perms.contains("*")) {
            return true;
        }
        return Arrays.stream(permissionKeys)
                .filter(k -> k != null && !k.isBlank())
                .anyMatch(k -> perms.contains(k.strip()));
    }

    public boolean hasAllPermissions(String... permissionKeys) {
        if (permissionKeys == null || permissionKeys.length == 0) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        if (isAdmin(auth)) {
            return true;
        }

        UserEffectivePermissionsResponse effective = getEffective(auth.getName());
        if (effective == null) {
            return false;
        }

        Set<String> perms = effective.permissions();
        if (perms.contains("*")) {
            return true;
        }
        return Arrays.stream(permissionKeys)
                .filter(k -> k != null && !k.isBlank())
                .allMatch(k -> perms.contains(k.strip()));
    }

    public boolean hasBranchAccess(String branchId) {
        if (branchId == null || branchId.isBlank()) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        if (isAdmin(auth)) {
            return true;
        }

        UserEffectivePermissionsResponse effective = getEffective(auth.getName());
        if (effective == null) {
            return false;
        }

        Set<String> branchScopes = effective.branchScopes();
        if (branchScopes.isEmpty() || branchScopes.contains("*")) {
            return true;
        }
        return branchScopes.contains(branchId.strip());
    }

    public boolean hasCostCenterAccess(String costCenterId) {
        if (costCenterId == null || costCenterId.isBlank()) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        if (isAdmin(auth)) {
            return true;
        }

        UserEffectivePermissionsResponse effective = getEffective(auth.getName());
        if (effective == null) {
            return false;
        }

        Set<String> costCenterScopes = effective.costCenterScopes();
        if (costCenterScopes.isEmpty() || costCenterScopes.contains("*")) {
            return true;
        }
        return costCenterScopes.contains(costCenterId.strip());
    }

    private boolean isAdmin(Authentication auth) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String authority = ga.getAuthority();
            if (("ROLE_" + RoleCode.SUPER_ADMIN.name()).equals(authority) ||
                ("ROLE_" + RoleCode.ADMIN.name()).equals(authority) ||
                RoleCode.SUPER_ADMIN.name().equals(authority) ||
                RoleCode.ADMIN.name().equals(authority)) {
                return true;
            }
        }
        return false;
    }

    private UserEffectivePermissionsResponse getEffective(String username) {
        if (username == null) {
            return null;
        }
        if (permissionCache != null) {
            UserEffectivePermissionsResponse cached = permissionCache.get(username);
            if (cached != null) {
                return cached;
            }
        }
        try {
            UserEffectivePermissionsResponse response = policyGroupService.getEffectivePermissions(username);
            if (response != null && permissionCache != null) {
                permissionCache.put(username, response);
            }
            return response;
        } catch (Exception ex) {
            log.warn("Failed to evaluate effective policy permissions for username={}", username, ex);
            return null;
        }
    }
}
