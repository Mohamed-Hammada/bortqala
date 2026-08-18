package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessApi;
import com.bemo.hr.access.domain.AccessCatalog;
import com.bemo.hr.access.domain.AccessDefs.AccessActionDef;
import com.bemo.hr.access.domain.AccessDefs.AccessConflictRuleDef;
import com.bemo.hr.access.domain.AccessDefs.AccessPageDef;
import com.bemo.hr.access.domain.AccessDefs.AccessRoleDef;
import com.bemo.hr.access.domain.AccessEnums.AccessLevel;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serves the canonical access catalog and computes the effective access preview.
 *
 * <p>All preview and validation logic is derived from {@link AccessCatalog}; no
 * frontend copy of the role/permission mapping exists, so the UI and the backend
 * can never drift apart. The backend remains authoritative: assignment validation
 * is re-run on every user save, including route-guard role parity, tenant feature
 * availability and acknowledgement of newly introduced risk.
 */
@Service
@Slf4j
public class AccessCatalogService {

    private static final List<String> LEVEL_PRECEDENCE = List.of(
            "REVERSE", "POST", "DECIDE", "APPROVE", "REVIEW", "MANAGE", "EDIT", "CREATE");

    private static final String ERR_UNKNOWN_MENU = "ACCESS_UNKNOWN_MENU";
    private static final String ERR_MENU_ROLE_MISMATCH = "ACCESS_MENU_ROLE_MISMATCH";
    private static final String ERR_FEATURE_DISABLED = "ACCESS_FEATURE_DISABLED";
    private static final String ERR_ACK_REASON_REQUIRED = "ACCESS_ACK_REASON_REQUIRED";

    private final AccessCatalog catalog;
    private final TenantFeatureService tenantFeatureService;

    public AccessCatalogService(AccessCatalog catalog, TenantFeatureService tenantFeatureService) {
        this.catalog = catalog;
        this.tenantFeatureService = tenantFeatureService;
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
        Set<String> enabledFeatures = enabledFeatures();

        List<AccessApi.EffectivePageAccessResponse> pageAccess = new ArrayList<>();
        for (AccessPageDef page : catalog.pages()) {
            pageAccess.add(effectiveAccess(page, selected, granted, menus, enabledFeatures));
        }

        List<AccessApi.AccessConflictResponse> conflicts = evaluateConflicts(granted, selected);
        List<AccessApi.AccessWarningResponse> warnings = sensitiveWarnings(granted);
        return new AccessApi.AccessPreviewResponse(pageAccess, warnings, conflicts,
                granted.stream().filter(catalog.sensitivePermissions()::contains).sorted().toList());
    }

    /**
     * Enforces delegation, segregation-of-duties and route/feature parity rules
     * for an assignment.
     *
     * <p>Blocking violations throw; recoverable issues are returned as
     * {@code valid=false} with a structured {@code errors} list so the UI can
     * surface each problem and collect the required acknowledgement reason.
     */
    public AccessApi.AccessValidateResponse validateAssignment(
            Set<String> actorRoles, String actorUserId,
            List<String> roleCodes, List<String> menuCodes,
            String targetUserId, Set<String> currentUserRoles, String reason) {
        validateRoleCodes(roleCodes);
        Set<String> selected = new LinkedHashSet<>(roleCodes);
        Set<String> granted = catalog.permissionsOfRoles(selected);
        Set<String> menus = menuCodes == null ? Set.of() : new LinkedHashSet<>(menuCodes);

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
        List<AccessApi.AccessValidateErrorResponse> errors =
                new ArrayList<>(menuAndFeatureErrors(menus, selected));
        // BORTQALA_FEEDBACK_20260816_ACCESS_REASON_OVERRIDE
        // A documented reason may acknowledge recoverable menu/feature parity mismatches.
        // Unknown menus and the hard security rules above remain non-bypassable.
        if (reason != null && !reason.isBlank()) {
            errors.removeIf(error -> ERR_MENU_ROLE_MISMATCH.equals(error.code())
                    || ERR_FEATURE_DISABLED.equals(error.code()));
        }
        errors.addAll(reasonErrors(conflicts, warnings, currentUserRoles, reason));

        return new AccessApi.AccessValidateResponse(errors.isEmpty(), conflicts, warnings, errors,
                granted.stream().filter(catalog.sensitivePermissions()::contains).sorted().toList());
    }

    /**
     * Runs {@link #validateAssignment} and throws when the assignment is not
     * valid, so user saves can never persist an assignment the catalog rejects.
     * The error carries the first machine key so the UI and the database-localized
     * exception handler can surface the precise problem.
     */
    public void validateAssignmentOrThrow(
            Set<String> actorRoles, String actorUserId,
            List<String> roleCodes, List<String> menuCodes,
            String targetUserId, Set<String> currentUserRoles, String reason) {
        AccessApi.AccessValidateResponse result = validateAssignment(
                actorRoles, actorUserId, roleCodes, menuCodes, targetUserId, currentUserRoles, reason);
        if (!result.valid()) {
            String firstErrorCode = result.errors().isEmpty() ? "ACCESS_ASSIGNMENT_INVALID"
                    : result.errors().get(0).code();
            throw new BusinessRuleException("The selected user access configuration is invalid.",
                    firstErrorCode, HttpStatus.CONFLICT);
        }
    }

    /**
     * Suggested minimal roles covering a set of business needs.
     */
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

    private Set<String> enabledFeatures() {
        return tenantFeatureService.getAllEnabled(TenantContext.require());
    }

    /**
     * Verifies every selected menu against the catalog: it must exist, its tenant
     * feature must be enabled, and at least one selected role must be able to open
     * the page's route.
     */
    private List<AccessApi.AccessValidateErrorResponse> menuAndFeatureErrors(Set<String> menus, Set<String> selected) {
        if (menus.isEmpty()) return List.of();
        Map<String, AccessPageDef> pageByMenuId = new HashMap<>();
        for (AccessPageDef page : catalog.pages()) {
            pageByMenuId.put(page.menuId(), page);
        }
        Set<String> enabledFeatures = enabledFeatures();
        List<AccessApi.AccessValidateErrorResponse> errors = new ArrayList<>();
        for (String menuId : menus) {
            AccessPageDef page = pageByMenuId.get(menuId);
            if (page == null) {
                errors.add(new AccessApi.AccessValidateErrorResponse(
                        ERR_UNKNOWN_MENU, "access.validate.unknownMenu", menuId, null));
                continue;
            }
            if (page.requiredFeature() != null && !enabledFeatures.contains(page.requiredFeature())) {
                errors.add(new AccessApi.AccessValidateErrorResponse(
                        ERR_FEATURE_DISABLED, "access.validate.featureDisabled", menuId, page.code()));
            }
            if (!page.requiredRoles().isEmpty() && Collections.disjoint(selected, page.requiredRoles())
                    && selected.stream().noneMatch(role -> "ADMIN".equals(role) || "SUPER_ADMIN".equals(role))) {
                errors.add(new AccessApi.AccessValidateErrorResponse(
                        ERR_MENU_ROLE_MISMATCH, "access.validate.menuRoleMismatch", menuId, page.code()));
            }
        }
        return errors;
    }

    /**
     * An acknowledgement reason is mandatory only when the new assignment
     * introduces warnings/conflicts the target user did not already carry, so
     * routine edits of admins are not blocked.
     */
    private List<AccessApi.AccessValidateErrorResponse> reasonErrors(
            List<AccessApi.AccessConflictResponse> conflicts,
            List<AccessApi.AccessWarningResponse> warnings,
            Set<String> currentUserRoles, String reason) {
        if (currentUserRoles == null || currentUserRoles.isEmpty()) {
            return List.of();
        }
        Set<String> baselineGranted = catalog.permissionsOfRoles(currentUserRoles);
        Set<String> baselineConflictCodes = evaluateConflicts(baselineGranted, currentUserRoles).stream()
                .map(AccessApi.AccessConflictResponse::code)
                .collect(Collectors.toSet());
        Set<String> baselineWarningCodes = sensitiveWarnings(baselineGranted).stream()
                .map(AccessApi.AccessWarningResponse::code)
                .collect(Collectors.toSet());

        boolean newConflict = conflicts.stream()
                .map(AccessApi.AccessConflictResponse::code)
                .anyMatch(code -> !baselineConflictCodes.contains(code));
        boolean newWarning = warnings.stream()
                .map(AccessApi.AccessWarningResponse::code)
                .anyMatch(code -> !baselineWarningCodes.contains(code));
        if ((newConflict || newWarning) && (reason == null || reason.isBlank())) {
            return List.of(new AccessApi.AccessValidateErrorResponse(
                    ERR_ACK_REASON_REQUIRED, "access.validate.ackReason", null, null));
        }
        return List.of();
    }

    private AccessApi.EffectivePageAccessResponse effectiveAccess(AccessPageDef page,
                                                                  Set<String> selectedRoles,
                                                                  Set<String> granted,
                                                                  Set<String> menus,
                                                                  Set<String> enabledFeatures) {
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

        boolean featureUnavailable = page.requiredFeature() != null
                && !enabledFeatures.contains(page.requiredFeature());
        boolean menuVisible = menus.contains(page.menuId());
        boolean adminSelected = selectedRoles.stream()
                .anyMatch(role -> "ADMIN".equals(role) || "SUPER_ADMIN".equals(role));
        boolean routeRoleDenied = !page.requiredRoles().isEmpty()
                && Collections.disjoint(selectedRoles, page.requiredRoles());
        String access;
        if (featureUnavailable) {
            access = AccessLevel.MODULE_UNAVAILABLE.name();
        } else if (adminSelected) {
            // Admin-level roles bypass menu selection and route-role checks but
            // remain subject to tenant feature flags above.
            access = AccessLevel.REVIEW.name();
        } else if (!menuVisible) {
            access = AccessLevel.HIDDEN.name();
        } else if (routeRoleDenied) {
            access = AccessLevel.RESTRICTED.name();
            missing.addAll(page.requiredRoles());
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

    public List<AccessApi.AccessPageResponse> availablePagesForUser(
            Set<String> roleCodes,
            Set<String> menuCodes) {
        AccessApi.AccessPreviewResponse preview = preview(
                roleCodes != null ? new ArrayList<>(roleCodes) : List.of(),
                menuCodes != null ? new ArrayList<>(menuCodes) : List.of());

        Set<String> allowedPageCodes = preview.pages().stream()
                .filter(page -> !"HIDDEN".equals(page.access()))
                .filter(page -> !"RESTRICTED".equals(page.access()))
                .filter(page -> !"MODULE_UNAVAILABLE".equals(page.access()))
                .map(AccessApi.EffectivePageAccessResponse::pageCode)
                .collect(Collectors.toSet());

        return catalog().pages().stream()
                .filter(page -> allowedPageCodes.contains(page.code()))
                .toList();
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
                page.requiredRoles().stream().sorted().toList(), page.requiredFeature(),
                page.actions().stream()
                        .map(action -> new AccessApi.AccessActionResponse(action.code(), action.permission(),
                                action.sensitive()))
                        .toList());
    }
}
