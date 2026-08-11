package com.bemo.hr.access.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Wire contracts for the role-to-page access guidance feature.
 */
public final class AccessApi {

    private AccessApi() {
    }

    public record AccessRoleResponse(String code, String nameKey, String descriptionKey,
                                     String sensitivity, String kind,
                                     List<String> permissions, List<String> dependencies,
                                     String sensitiveReasonKey) { }

    public record AccessActionResponse(String code, String permission, boolean sensitive) { }

    public record AccessPageResponse(String code, String module, String route, String menuId,
                                     String titleKey, List<String> viewPermissions,
                                     List<String> roles, String requiredFeature,
                                     List<AccessActionResponse> actions) { }

    public record AccessConflictRuleResponse(String code, List<String> permissions,
                                             String severity, String reasonKey) { }

    public record AccessNeedResponse(String code, String labelKey, List<String> permissions) { }

    public record AccessCatalogResponse(List<AccessRoleResponse> roles,
                                        List<AccessPageResponse> pages,
                                        List<AccessConflictRuleResponse> conflictRules,
                                        List<AccessNeedResponse> needs,
                                        List<String> sensitivePermissions) { }

    public record AccessPreviewRequest(
            @NotEmpty List<String> roleCodes,
            List<String> menuCodes) { }

    public record EffectivePageAccessResponse(String pageCode, String access,
                                              List<String> grantedByRoles,
                                              List<String> grantedActions,
                                              List<String> missingPermissions) { }

    public record AccessWarningResponse(String code, String messageKey,
                                        List<String> permissions, boolean blocking) { }

    public record AccessConflictResponse(String code, String reasonKey,
                                         List<String> roles, List<String> permissions,
                                         String severity) { }

    public record AccessPreviewResponse(List<EffectivePageAccessResponse> pages,
                                        List<AccessWarningResponse> warnings,
                                        List<AccessConflictResponse> conflicts,
                                        List<String> sensitivePermissions) { }

    public record AccessValidateRequest(
            @NotEmpty List<String> roleCodes,
            List<String> menuCodes,
            @Size(max = 36) String targetUserId,
            @Size(max = 500) String reason) { }

    public record AccessValidateErrorResponse(String code, String messageKey,
                                              String menuId, String pageCode) { }

    public record AccessValidateResponse(boolean valid,
                                         List<AccessConflictResponse> conflicts,
                                         List<AccessWarningResponse> warnings,
                                         List<AccessValidateErrorResponse> errors,
                                         List<String> sensitivePermissions) { }
}
