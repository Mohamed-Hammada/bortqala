package com.bemo.hr.access.sso.application;

import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.JwtProperties;
import com.bemo.hr.shared.security.RefreshTokenService;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Issues the same JWT session pipeline used by password login for an SSO-authenticated user:
 * an HMAC-signed access token carrying the standard claims (appId/appCode/tv/pwc/roles) plus a
 * refresh token, so an SSO login yields a fully usable session (WP-34 AC-1).
 */
@Component
@RequiredArgsConstructor
public class SsoSessionIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final RefreshTokenService refreshTokenService;

    public SsoApi.CallbackResult issue(AppUser user, boolean newlyProvisioned, Instant now) {
        TenantApplication app = tenantApplicationRepository.findById(user.getAppId())
                .orElseThrow(() -> new NotFoundException("Application not found.", "APP_NOT_FOUND"));
        Duration ttl = jwtProperties.ttl();
        if (app.isSessionTimeoutEnabled() && app.getSessionTimeoutMinutes() > 0) {
            ttl = Duration.ofMinutes(app.getSessionTimeoutMinutes());
        }
        Instant expiresAt = now.plus(ttl);
        boolean passwordChangeRequired = user.isMustChangePassword();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("appId", app.getId())
                .claim("appCode", app.getCode())
                .claim("name", user.getDisplayName())
                .claim("tv", user.getTokenVersion())
                .claim("pwc", passwordChangeRequired)
                .claim("roles", passwordChangeRequired
                        ? List.of()
                        : user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokenService.issue(app.getId(), user.getId(), "sso");
        return new SsoApi.CallbackResult(
                user.getId(),
                newlyProvisioned,
                accessToken,
                refresh.rawValue(),
                expiresAt.getEpochSecond() - now.getEpochSecond(),
                user.getUsername(),
                user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList()
        );
    }
}