package com.bemo.hr.shared.security;

import com.bemo.hr.shared.domain.NotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

/**
 * Exchange endpoint for the demo no-login link. It always exists so that the
 * route is stable, but it returns {@code 404} unless the demo feature is
 * available and the presented secret matches the one generated at startup.
 */
@RestController
@RequestMapping("/api/v1")
public class DemoNoLoginController {
    private final DemoNoLoginService demoNoLoginService;
    private final AuthService authService;
    private final RefreshCookieCodec refreshCookieCodec;
    private final String refreshCookieName;
    private final boolean refreshCookieSecure;

    public DemoNoLoginController(DemoNoLoginService demoNoLoginService,
                                 AuthService authService,
                                 RefreshCookieCodec refreshCookieCodec,
                                 @Value("${hr.security.refresh-cookie-name:bemo_refresh}") String refreshCookieName,
                                 @Value("${hr.security.refresh-cookie-secure:true}") boolean refreshCookieSecure) {
        this.demoNoLoginService = demoNoLoginService;
        this.authService = authService;
        this.refreshCookieCodec = refreshCookieCodec;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/auth/demo-login")
    ResponseEntity<AuthApi.LoginResponse> demoLogin(@RequestBody(required = false) AuthApi.DemoLoginRequest request,
                                                    HttpServletResponse servletResponse,
                                                    @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        if (!demoNoLoginService.isValidSecret(request == null ? null : request.secret())) {
            throw new NotFoundException("The demo superadmin link is invalid or has expired.",
                    "DEMO_NO_LOGIN_LINK_INVALID");
        }
        AuthService.LoginResult result = authService.demoSuperadminLogin(deviceId);
        setRefreshCookie(servletResponse, refreshCookieCodec.encode(result.appId(), result.refreshToken()),
                result.refreshExpiresAt());
        return ResponseEntity.ok(result.response());
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
}
