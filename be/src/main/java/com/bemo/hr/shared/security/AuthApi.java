package com.bemo.hr.shared.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class AuthApi {
    private AuthApi() {
    }

    public record LoginRequest(@NotBlank @Size(max = 50) String appCode,
                               @NotBlank String username, @NotBlank String password) { }
    public record AppResponse(String id, String code, String name,
                              boolean adminDashboardCustomizationEnabled) { }
    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt,
                                boolean mustChangePassword,
                                AppResponse app, UserResponse user, PreferenceResponse preferences) { }
    public record RefreshResponse(String accessToken, String tokenType, Instant expiresAt) { }
    public record ChangePasswordRequest(@NotBlank @Size(max = 72) String currentPassword,
                                        @NotBlank @Size(max = 72) String newPassword) { }
    public record UserResponse(String id, String username, String displayName, Set<RoleCode> roles,
                               Set<String> allowedMenus, boolean canViewSalary, String categoryId,
                               boolean dashboardCustomizationEnabled, boolean active, long version) { }
    public record TenantInfo(String id, String code, String name) { }
    public record SessionInfo(Instant expiresAt, int timeoutMinutes, boolean timeoutEnabled) { }
    public record MeResponse(String id, String username, String displayName,
                             TenantInfo tenant, Set<RoleCode> roles, Set<String> scopes,
                             boolean canViewSalary, String categoryId,
                             boolean dashboardCustomizationEnabled, boolean active,
                             SessionInfo session, long version) { }
    public record UserUpsertRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 150) String displayName,
            @Size(max = 72) String password,
            Set<RoleCode> roles,
            Set<String> allowedMenus,
            Boolean canViewSalary,
            String categoryId,
            Boolean dashboardCustomizationEnabled,
            boolean active,
            Long version) { }
    public record PreferenceResponse(ThemePreference theme, TableDensity tableDensity,
                                     String locale, ExcelTableStyle excelTableStyle, int defaultPageSize,
                                     String defaultPage, boolean showFavorites, boolean showRecentlyUsed,
                                     int maxRecentlyUsed, Set<String> favoriteMenuIds, Set<String> recentMenuIds,
                                     List<String> dashboardWidgetIds, boolean dashboardAnimationsEnabled,
                                     boolean dashboardLayoutCustomizationAllowed,
                                     Instant updatedAt) { }
    public record PreferenceRequest(@NotNull ThemePreference theme, @NotNull TableDensity tableDensity,
                                    @NotBlank @Pattern(regexp = "[a-zA-Z]{2}(-[a-zA-Z]{2})?") String locale,
                                    @NotNull ExcelTableStyle excelTableStyle,
                                    @Min(5) @Max(500) Integer defaultPageSize,
                                    String defaultPage) { }
    public record NavigationPreferenceRequest(
            boolean showFavorites,
            boolean showRecentlyUsed,
            @Min(1) @Max(20) int maxRecentlyUsed,
            Set<@Pattern(regexp = "[a-z0-9-]{1,80}") String> favoriteMenuIds,
            Set<@Pattern(regexp = "[a-z0-9-]{1,80}") String> recentMenuIds) { }
    public record DashboardPreferenceRequest(
            @NotNull @Size(min = 1, max = 8)
            List<@Pattern(regexp = "summary|report|attendance-chart|insights|units|departments|categories|imports") String> widgetIds,
            boolean animationsEnabled) { }
    public record AppSettingsResponse(
            int sessionTimeoutMinutes, boolean sessionTimeoutEnabled, boolean showReportPresets,
            int attendanceAnomalyThresholdPercent,
            boolean automaticProcurementNumbering, boolean adminDashboardCustomizationEnabled,
            int minPasswordLength, boolean requireUppercase, boolean requireLowercase,
            boolean requireNumbers, boolean requireSpecialChars, boolean disallowSpaces,
            int maxPasswordLength, int passwordExpiryDays, int passwordHistoryCount,
            Instant updatedAt) { }
    public record UserCategoryResponse(String id, String code, String name, String scope) { }

    public record AppSettingsRequest(
            @Min(5) @Max(10_080) int sessionTimeoutMinutes,
            boolean sessionTimeoutEnabled,
            boolean showReportPresets,
            @Min(1) @Max(100) int attendanceAnomalyThresholdPercent,
            boolean automaticProcurementNumbering,
            boolean adminDashboardCustomizationEnabled,
            @Min(6) @Max(128) Integer minPasswordLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireNumbers,
            boolean requireSpecialChars,
            boolean disallowSpaces,
            @Min(0) @Max(256) Integer maxPasswordLength,
            @Min(0) @Max(365) Integer passwordExpiryDays,
            @Min(0) @Max(50) Integer passwordHistoryCount) { }
}
