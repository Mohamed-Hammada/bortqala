package com.bemo.hr.security.pack.api;

import com.bemo.hr.security.pack.application.TotpService;
import com.bemo.hr.shared.security.AuthApi;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.shared.security.ClientIpResolver;
import com.bemo.hr.shared.security.RefreshCookieCodec;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/2fa")
public class Auth2FaController {
    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(30);

    private final TotpService totpService;
    private final AuthService authService;
    private final RefreshCookieCodec refreshCookieCodec;
    private final ClientIpResolver clientIpResolver;
    private final String refreshCookieName;
    private final boolean refreshCookieSecure;

    public Auth2FaController(TotpService totpService,
                             AuthService authService,
                             RefreshCookieCodec refreshCookieCodec,
                             ClientIpResolver clientIpResolver,
                             @Value("${hr.security.refresh-cookie-name:bemo_refresh}") String refreshCookieName,
                             @Value("${hr.security.refresh-cookie-secure:true}") boolean refreshCookieSecure) {
        this.totpService = totpService;
        this.authService = authService;
        this.refreshCookieCodec = refreshCookieCodec;
        this.clientIpResolver = clientIpResolver;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SecurityPackApi.TotpStatusResponse> getStatus(Authentication authentication) {
        String appId = TenantContext.require();
        String userId = authService.getUserIdByUsername(appId, authentication.getName());
        TotpService.TotpStatusResult result = totpService.getStatus(appId, userId);
        return ResponseEntity.ok(new SecurityPackApi.TotpStatusResponse(
                result.enabled(),
                result.enabledAt(),
                result.remainingBackupCodes()
        ));
    }

    @PostMapping("/enroll")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SecurityPackApi.TotpEnrollResponse> enroll(Authentication authentication) {
        String appId = TenantContext.require();
        String userId = authService.getUserIdByUsername(appId, authentication.getName());
        TotpService.EnrollResult result = totpService.enroll(appId, userId);
        return ResponseEntity.ok(new SecurityPackApi.TotpEnrollResponse(
                result.secret(),
                result.otpauthUri(),
                result.backupCodes()
        ));
    }

    @PostMapping("/activate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> activate(@Valid @RequestBody SecurityPackApi.TotpActivateRequest request,
                                         Authentication authentication) {
        String appId = TenantContext.require();
        String userId = authService.getUserIdByUsername(appId, authentication.getName());
        totpService.activate(appId, userId, request.code());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disable(@Valid @RequestBody SecurityPackApi.TotpDisableRequest request,
                                        Authentication authentication) {
        String appId = TenantContext.require();
        String userId = authService.getUserIdByUsername(appId, authentication.getName());
        totpService.disable(appId, userId, request.password());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/backup-codes/regenerate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> regenerateBackupCodes(@Valid @RequestBody SecurityPackApi.RegenerateBackupCodesRequest request,
                                                              Authentication authentication) {
        String appId = TenantContext.require();
        String userId = authService.getUserIdByUsername(appId, authentication.getName());
        List<String> codes = totpService.regenerateBackupCodes(appId, userId, request.codeOrPassword());
        return ResponseEntity.ok(codes);
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthApi.LoginResponse> verify2fa(@Valid @RequestBody SecurityPackApi.TotpVerifyRequest request,
                                                           HttpServletRequest servletRequest,
                                                           HttpServletResponse servletResponse,
                                                           @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        String userAgent = servletRequest.getHeader(HttpHeaders.USER_AGENT);
        String ip = clientIpResolver.resolve(servletRequest);
        AuthService.LoginResult result = authService.verify2faLogin(request.challengeToken(), request.code(), deviceId, userAgent, ip);
        if (result.refreshToken() != null) {
            setRefreshCookie(servletResponse, refreshCookieCodec.encode(result.appId(), result.refreshToken()), result.refreshExpiresAt());
        }
        return ResponseEntity.ok(result.response());
    }

    private void setRefreshCookie(HttpServletResponse response, String value, Instant expiresAt) {
        long maxAge = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (maxAge <= 0) {
            maxAge = REFRESH_COOKIE_MAX_AGE.toSeconds();
        }
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
