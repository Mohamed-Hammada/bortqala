package com.bemo.hr.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(30);

    private final AuthService authService;
    private final RefreshCookieCodec refreshCookieCodec;
    private final ClientIpResolver clientIpResolver;
    private final String refreshCookieName;
    private final boolean refreshCookieSecure;

    public AuthController(AuthService authService,
                          RefreshCookieCodec refreshCookieCodec,
                          ClientIpResolver clientIpResolver,
                          @Value("${hr.security.refresh-cookie-name:bemo_refresh}") String refreshCookieName,
                          @Value("${hr.security.refresh-cookie-secure:true}") boolean refreshCookieSecure) {
        this.authService = authService;
        this.refreshCookieCodec = refreshCookieCodec;
        this.clientIpResolver = clientIpResolver;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/auth/login")
    ResponseEntity<AuthApi.LoginResponse> login(@Valid @RequestBody AuthApi.LoginRequest request,
                                                HttpServletRequest servletRequest,
                                                HttpServletResponse servletResponse,
                                                @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        AuthService.LoginResult result = authService.login(request, deviceId, clientIpResolver.resolve(servletRequest));
        setRefreshCookie(servletResponse, refreshCookieCodec.encode(result.appId(), result.refreshToken()),
                result.refreshExpiresAt());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<AuthApi.RefreshResponse> refresh(HttpServletRequest servletRequest,
                                                    HttpServletResponse servletResponse,
                                                    @CookieValue(name = "${hr.security.refresh-cookie-name:bemo_refresh}", required = false) String refreshCookie,
                                                    @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        AuthService.RefreshResult result = authService.refresh(refreshCookie, deviceId);
        setRefreshCookie(servletResponse, refreshCookieCodec.encode(result.appId(), result.refreshToken()),
                result.refreshExpiresAt());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(HttpServletResponse servletResponse,
                                @CookieValue(name = "${hr.security.refresh-cookie-name:bemo_refresh}", required = false) String refreshCookie) {
        try {
            if (refreshCookie != null && !refreshCookie.isBlank()) {
                authService.logout(refreshCookie);
            }
        } finally {
            clearRefreshCookie(servletResponse);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/change-password")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<Void> changePassword(@Valid @RequestBody AuthApi.ChangePasswordRequest request,
                                        HttpServletResponse servletResponse,
                                        Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        clearRefreshCookie(servletResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/revoke-sessions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeSessions(@PathVariable String id, Authentication authentication) {
        authService.revokeSessions(id, authentication.getName());
    }

    @PostMapping("/users/{id}/unlock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unlock(@PathVariable String id, Authentication authentication) {
        authService.unlock(id, authentication.getName());
    }

    @GetMapping("/auth/me")
    AuthApi.UserResponse me(Authentication authentication) { return authService.current(authentication.getName()); }

    @GetMapping("/users/me")
    AuthApi.MeResponse usersMe(@AuthenticationPrincipal Jwt jwt) {
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    AuthApi.AppSettingsResponse appSettings() { return authService.currentAppSettings(); }

    @PutMapping("/admin/app-settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    AuthApi.AppSettingsResponse updateAppSettings(@Valid @RequestBody AuthApi.AppSettingsRequest request,
                                                  Authentication authentication) {
        return authService.updateAppSettings(request, authentication.getName());
    }

    @GetMapping("/auth/user-categories")
    List<AuthApi.UserCategoryResponse> userCategories() { return authService.listCategories(); }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    List<AuthApi.UserResponse> users() { return authService.listUsers(); }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    AuthApi.UserResponse create(@Valid @RequestBody AuthApi.UserUpsertRequest request,
                                Authentication authentication) {
        return authService.create(request, authentication.getName());
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    AuthApi.UserResponse update(@PathVariable String id, @Valid @RequestBody AuthApi.UserUpsertRequest request,
                                Authentication authentication) {
        return authService.update(id, request, authentication.getName());
    }

    private void setRefreshCookie(HttpServletResponse servletResponse, String refreshToken, Instant expiresAt) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.between(Instant.now(), expiresAt))
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse servletResponse) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
