package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessApi;
import com.bemo.hr.access.domain.AccessCatalog;
import com.bemo.hr.access.domain.AccessDefs.AccessActionDef;
import com.bemo.hr.access.domain.AccessDefs.AccessConflictRuleDef;
import com.bemo.hr.access.domain.AccessDefs.AccessPageDef;
import com.bemo.hr.access.domain.AccessDefs.AccessRoleDef;
import com.bemo.hr.access.domain.AccessEnums.AccessLevel;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Serves the canonical access catalog and computes the effective access preview.
 *
 * <p>All preview and validation logic is derived from {@link AccessCatalog}; no
 * frontend copy of the role/permission mapping exists, so the UI and the backend
 * can never drift apart. The backend remains authoritative: assignment validation
 * is re-run on every user save.
 */
@Service
public class AccessCatalogService {

    private static final List<String> LEVEL_PRECEDENCE = List.of(
            "REVERSE", "POST", "APPROVE", "REVIEW", "MANAGE", "EDIT", "CREATE");

    private final AccessCatalog catalog;

    public AccessCatalogService(AccessCatalog catalog) {
        this.catalog = catalog;
    }

    public AccessApi.AccessCatalogResponse catalog() {
        List<AccessApi.AccessRoleResponse> roles = catalog.roles().stream().map(this::toRole).toList();
        List<AccessApi.AccessPageResponse> pages = catalog.pages().stream().map(this::toPage).toList();
        List<AccessApi.AccessConflictRuleResponse> rules = catalog.conflictRules().stream()
                .map(rule -> new AccessApi.AccessConflictRuleResponse(
                        rule.code(), rule.permissions(), rule.severity().name(), rule.reasonKey()))
                .toList();
        List<AccessApi.AccessNeedResponse> needs = catalog.needs().stream()
                .map(need -> new AccessApi.AccessNeedResponse(need.code(), need.labelKey(),
                        need.permissions().stream().sorted().toList()))
                .toList();
        return new AccessApi.AccessCatalogResponse(roles, pages, rules, needs,
                catalog.sensitivePermissions().stream().sorted().toList());
    }

    public AccessApi.AccessPreviewResponse preview(List<String> roleCodes, List<String> menuCodes) {
        validateRoleCodes(roleCodes);
        Set<String> selected = new LinkedHashSet<>(roleCodes);
        Set<String> granted = catalog.permissionsOfRoles(selected);
        Set<String> menus = menuCodes == null ? Set.of() : new LinkedHashSet<>(menuCodes);

        List<AccessApi.EffectivePageAccessResponse> pageAccess = new ArrayList<>();
        for (AccessPageDef page : catalog.pages()) {
            pageAccess.add(effectiveAccess(page, selected, granted, menus));
        }

        List<AccessApi.AccessConflictResponse> conflicts = evaluateConflicts(granted, selected);
        List<AccessApi.AccessWarningResponse> warnings = sensitiveWarnings(granted);
        return new AccessApi.AccessPreviewResponse(pageAccess, warnings, conflicts,
                granted.stream().filter(catalog.sensitivePermissions()::contains).sorted().toList());
    }

    /**
     * Enforces delegation and segregation-of-duties rules for an assignment.
     *
     * <p>Blocking violations throw; warning-only violations are returned so the
     * administrator can acknowledge them before saving.
     */
    public AccessApi.AccessValidateResponse validateAssignment(
            Set<String> actorRoles, String actorUserId,
            List<String> roleCodes, List<String> menuCodes,
            String targetUserId, Set<String> currentUserRoles, String reason) {
        validateRoleCodes(roleCodes);
        Set<String> selected = new LinkedHashSet<>(roleCodes);
        Set<String> granted = catalog.permissionsOfRoles(selected);

        if (!actorRoles.contains("SUPER_ADMIN") && selected.contains("SUPER_ADMIN")) {
            throw new BusinessRuleException("Only a Super Admin can assign the Super Admin role.",
                    "AUTH_SUPER_ADMIN_ROLE_ASSIGNMENT_FORBIDDEN", HttpStatus.CONFLICT);
        }
        if (targetUserId != null && targetUserId.equals(actorUserId) && currentUserRoles != null
                && !currentUserRoles.equals(selected)) {
            throw new BusinessRuleException("You cannot change your own roles.",
                    "ACCESS_SELF_ROLE_MODIFICATION", HttpStatus.CONFLICT);
        }

        List<AccessApi.AccessConflictResponse> conflicts = evaluateConflicts(granted, selected);
        for (AccessApi.AccessConflictResponse conflict : conflicts) {
            if ("BLOCK".equals(conflict.severity())) {
                throw new BusinessRuleException("The selected roles violate a segregation-of-duties rule.",
                        "ACCESS_CONFLICT_BLOCKED", HttpStatus.CONFLICT);
            }
        }

        List<AccessApi.AccessWarningResponse> warnings = sensitiveWarnings(granted);
        return new AccessApi.AccessValidateResponse(true, conflicts, warnings,
                granted.stream().filter(catalog.sensitivePermissions()::contains).sorted().toList());
    }

    /** Suggested minimal roles covering a set of business needs. */
    public List<String> suggestRoles(Set<String> permissions) {
        Set<String> uncovered = new HashSet<>(permissions);
        if (uncovered.isEmpty()) return List.of();
        Set<String> chosen = new LinkedHashSet<>();
        while (!uncovered.isEmpty()) {
            String best = null;
            int bestCover = 0;
            int bestSize = Integer.MAX_VALUE;
            for (AccessRoleDef role : catalog.roles()) {
                if ("ADMIN".equals(role.code()) || "SUPER_ADMIN".equals(role.code())) {
                    continue;
                }
                Set<String> granted = role.permissions();
                int cover = 0;
                for (String permission : uncovered) {
                    if (granted.contains(permission)) cover++;
                }
                if (cover == 0) {
                    continue;
                }
                if (cover > bestCover || (cover == bestCover && granted.size() < bestSize)) {
                    bestCover = cover;
                    bestSize = granted.size();
                    best = role.code();
                }
            }
            if (best == null) {
                break;
            }
            chosen.add(best);
            uncovered.removeAll(catalog.permissionsOf(best));
        }
        return List.copyOf(chosen);
    }

    private void validateRoleCodes(List<String> roleCodes) {
        for (String code : roleCodes) {
            if (!catalog.hasRole(code)) {
                throw new BusinessRuleException("Unknown role code: " + code,
                        "ACCESS_UNKNOWN_ROLE", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private AccessApi.EffectivePageAccessResponse effectiveAccess(AccessPageDef page,
                                                                  Set<String> selectedRoles,
                                                                  Set<String> granted,
                                                                  Set<String> menus) {
        boolean viewGranted = granted.contains(page.viewPermission());
        Set<String> grantedActions = new LinkedHashSet<>();
        for (AccessActionDef action : page.actions()) {
            if (granted.contains(action.permission())) {
                grantedActions.add(action.code());
            }
        }
        Set<String> missing = new LinkedHashSet<>();
        if (!viewGranted) {
            missing.add(page.viewPermission());
        }
        for (AccessActionDef action : page.actions()) {
            if (!granted.contains(action.permission())) {
                missing.add(action.permission());
            }
        }

        List<String> grantingRoles = selectedRoles.stream()
                .filter(role -> catalog.permissionsOf(role).contains(page.viewPermission()))
                .sorted()
                .toList();

        boolean menuVisible = menus.contains(page.menuId());
        String access;
        if (!menuVisible) {
            access = AccessLevel.HIDDEN.name();
        } else if (!viewGranted) {
            access = AccessLevel.RESTRICTED.name();
        } else {
            access = deriveLevel(grantedActions);
        }
        return new AccessApi.EffectivePageAccessResponse(page.code(), access, grantingRoles,
                grantedActions.stream().sorted().toList(), missing.stream().sorted().toList());
    }

    private String deriveLevel(Set<String> grantedActions) {
        for (String level : LEVEL_PRECEDENCE) {
            if (grantedActions.contains(level)) {
                return level;
            }
        }
        return AccessLevel.VIEW.name();
    }

    private List<AccessApi.AccessConflictResponse> evaluateConflicts(Set<String> granted, Set<String> selectedRoles) {
        List<AccessApi.AccessConflictResponse> result = new ArrayList<>();
        for (AccessConflictRuleDef rule : catalog.conflictRules()) {
            if (granted.containsAll(rule.permissions())) {
                List<String> affected = rule.permissions().stream()
                        .flatMap(permission -> catalog.rolesGranting(permission).stream())
                        .filter(selectedRoles::contains)
                        .distinct()
                        .sorted()
                        .toList();
                result.add(new AccessApi.AccessConflictResponse(rule.code(), rule.reasonKey(),
                        affected, rule.permissions(), rule.severity().name()));
            }
        }
        return result;
    }

    private List<AccessApi.AccessWarningResponse> sensitiveWarnings(Set<String> granted) {
        List<AccessApi.AccessWarningResponse> warnings = new ArrayList<>();
        for (String permission : granted.stream().sorted().toList()) {
            if (!catalog.sensitivePermissions().contains(permission)) {
                continue;
            }
            String messageKey = "access.warnings." + permission.replace('.', '-');
            warnings.add(new AccessApi.AccessWarningResponse(permission, messageKey,
                    List.of(permission), false));
        }
        return warnings;
    }

    private AccessApi.AccessRoleResponse toRole(AccessRoleDef role) {
        return new AccessApi.AccessRoleResponse(role.code(), role.nameKey(), role.nameKey() + ".description",
                role.sensitivity().name(), role.kind().name(),
                role.permissions().stream().sorted().toList(),
                role.dependencies().stream().sorted().toList(),
                role.sensitiveReasonKey());
    }

    private AccessApi.AccessPageResponse toPage(AccessPageDef page) {
        return new AccessApi.AccessPageResponse(page.code(), page.module(), page.route(), page.menuId(),
                page.titleKey(), List.of(page.viewPermission()),
                page.actions().stream()
                        .map(action -> new AccessApi.AccessActionResponse(action.code(), action.permission(),
                                action.sensitive()))
                        .toList());
    }
}
