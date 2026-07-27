package com.bemo.hr.shared.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public final class AuthApi {
    private AuthApi() {
    }

    public record LoginRequest(@NotBlank @Size(max = 50) String appCode,
                               @NotBlank String username, @NotBlank String password) { }
    public record AppResponse(String id, String code, String name) { }
    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt,
                                AppResponse app, UserResponse user, PreferenceResponse preferences) { }
    public record UserResponse(String id, String username, String displayName, Set<RoleCode> roles, Set<String> allowedMenus, boolean canViewSalary, boolean active, long version) { }
    public record UserUpsertRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 150) String displayName,
            @Size(max = 72) String password,
            Set<RoleCode> roles,
            Set<String> allowedMenus,
            Boolean canViewSalary,
            boolean active,
            Long version) { }
    public record PreferenceResponse(ThemePreference theme, TableDensity tableDensity,
                                     String locale, ExcelTableStyle excelTableStyle, int defaultPageSize, Instant updatedAt) { }
    public record PreferenceRequest(@NotNull ThemePreference theme, @NotNull TableDensity tableDensity,
                                    @NotBlank @Pattern(regexp = "[a-zA-Z]{2}(-[a-zA-Z]{2})?") String locale,
                                    @NotNull ExcelTableStyle excelTableStyle,
                                    @Min(5) @Max(500) Integer defaultPageSize) { }
    public record AppSettingsResponse(int sessionTimeoutMinutes, boolean sessionTimeoutEnabled, boolean showReportPresets, int minPasswordLength, Instant updatedAt) { }
    public record AppSettingsRequest(@Min(5) @Max(10_080) int sessionTimeoutMinutes, boolean sessionTimeoutEnabled, boolean showReportPresets, @Min(6) @Max(32) Integer minPasswordLength) { }
}
