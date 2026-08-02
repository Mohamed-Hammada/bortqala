package com.bemo.hr.shared.security;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/auth/login")
    AuthApi.LoginResponse login(@Valid @RequestBody AuthApi.LoginRequest request) { return authService.login(request); }

    @GetMapping("/auth/me")
    AuthApi.UserResponse me(Authentication authentication) { return authService.current(authentication.getName()); }

    @GetMapping("/users/me")
    AuthApi.MeResponse usersMe(org.springframework.security.oauth2.jwt.Jwt jwt) {
        return authService.me(jwt.getSubject(), jwt.getExpiresAt());
    }

    @GetMapping("/auth/preferences")
    AuthApi.PreferenceResponse preferences(Authentication authentication) {
        return authService.currentPreferences(authentication.getName());
    }

    @PutMapping("/auth/preferences")
    AuthApi.PreferenceResponse updatePreferences(@Valid @RequestBody AuthApi.PreferenceRequest request,
                                                 Authentication authentication) {
        return authService.updatePreferences(authentication.getName(), request);
    }

    @PutMapping("/auth/preferences/navigation")
    AuthApi.PreferenceResponse updateNavigationPreferences(
            @Valid @RequestBody AuthApi.NavigationPreferenceRequest request, Authentication authentication) {
        return authService.updateNavigationPreferences(authentication.getName(), request);
    }

    @PutMapping("/auth/preferences/dashboard")
    AuthApi.PreferenceResponse updateDashboardPreferences(
            @Valid @RequestBody AuthApi.DashboardPreferenceRequest request, Authentication authentication) {
        return authService.updateDashboardPreferences(authentication.getName(), request);
    }

    @GetMapping("/admin/app-settings")
    @PreAuthorize("hasRole('ADMIN')")
    AuthApi.AppSettingsResponse appSettings() { return authService.currentAppSettings(); }

    @PutMapping("/admin/app-settings")
    @PreAuthorize("hasRole('ADMIN')")
    AuthApi.AppSettingsResponse updateAppSettings(@Valid @RequestBody AuthApi.AppSettingsRequest request,
                                                  Authentication authentication) {
        return authService.updateAppSettings(request, authentication.getName());
    }

    @GetMapping("/auth/user-categories")
    List<AuthApi.UserCategoryResponse> userCategories() { return authService.listCategories(); }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    List<AuthApi.UserResponse> users() { return authService.listUsers(); }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    AuthApi.UserResponse create(@Valid @RequestBody AuthApi.UserUpsertRequest request) { return authService.create(request); }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    AuthApi.UserResponse update(@PathVariable String id, @Valid @RequestBody AuthApi.UserUpsertRequest request,
                                Authentication authentication) {
        return authService.update(id, request, authentication.getName());
    }
}
