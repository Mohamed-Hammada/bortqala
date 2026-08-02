package com.bemo.hr.shared.security;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {
    private static final Duration REUSE_DETECTION_GRACE = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final AppUserRepository appUserRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               TenantApplicationRepository tenantApplicationRepository,
                               AppUserRepository appUserRepository,
                               JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.appUserRepository = appUserRepository;
        this.jwtProperties = jwtProperties;
    }

    public IssuedRefreshToken issue(String appId, String userId, String deviceId) {
        String raw = generateToken();
        String familyId = UUID.randomUUID().toString();
        Duration ttl = refreshTtl();
        RefreshToken entity = new RefreshToken(appId, userId, familyId, hash(raw), Instant.now().plus(ttl), deviceId);
        refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(raw, entity.getId(), entity.getExpiresAt());
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RotationResult rotate(String appId, String rawToken, String deviceId, String by) {
        RefreshToken existing = refreshTokenRepository.findForRotationByAppIdAndTokenHash(appId, hash(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Session is invalid or expired.",
                        "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED));
        Instant now = Instant.now();
        if (existing.getRevokedAt() != null || existing.getReplacedByTokenId() != null
                || !existing.getExpiresAt().isAfter(now)) {
            revokeFamily(appId, existing.getFamilyId(), by);
            throw new BusinessRuleException("Session is invalid or expired.",
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        String raw = generateToken();
        RefreshToken replacement = new RefreshToken(appId, existing.getUserId(), existing.getFamilyId(),
                hash(raw), now.plus(refreshTtl()), deviceId);
        refreshTokenRepository.save(replacement);
        existing.markReplacedBy(replacement.getId());
        return new RotationResult(existing.getUserId(), raw, replacement.getExpiresAt());
    }

    public RefreshToken requireActive(String appId, String rawToken) {
        RefreshToken token = refreshTokenRepository.findByAppIdAndTokenHash(appId, hash(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Session is invalid or expired.",
                        "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED));
        if (token.getRevokedAt() != null || token.getReplacedByTokenId() != null
                || !token.getExpiresAt().isAfter(Instant.now())) {
            throw new BusinessRuleException("Session is invalid or expired.",
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        return token;
    }

    public String userIdFor(String appId, String rawToken) {
        return requireActive(appId, rawToken).getUserId();
    }

    public String usernameFor(String appId, String rawToken) {
        return refreshTokenRepository.findByAppIdAndTokenHash(appId, hash(rawToken))
                .map(RefreshToken::getUserId)
                .flatMap(userId -> appUserRepository.findById(userId))
                .map(AppUser::getUsername)
                .orElse("logout");
    }

    public void revoke(String appId, String rawToken, String by) {
        refreshTokenRepository.findByAppIdAndTokenHash(appId, hash(rawToken))
                .ifPresent(token -> token.revoke(by));
    }

    public void revokeAllForUser(String appId, String userId, String by) {
        List<RefreshToken> active = refreshTokenRepository.findAllByAppIdAndUserIdAndRevokedAtIsNull(appId, userId);
        active.forEach(token -> token.revoke(by));
    }

    public void revokeAllForApp(String appId, String by) {
        refreshTokenRepository.findAllByAppIdAndRevokedAtIsNull(appId).forEach(token -> token.revoke(by));
    }

    @Scheduled(initialDelayString = "${hr.security.refresh-cleanup-initial-delay-ms:3600000}",
            fixedDelayString = "${hr.security.refresh-cleanup-interval-ms:3600000}")
    public void cleanupExpiredAndRevoked() {
        Instant cutoff = Instant.now().minus(REUSE_DETECTION_GRACE);
        for (TenantApplication app : tenantApplicationRepository.findAll()) {
            TenantContext.set(app.getId());
            try {
                refreshTokenRepository.deleteExpiredBefore(app.getId(), cutoff);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void revokeFamily(String appId, String familyId, String by) {
        refreshTokenRepository.findAllByAppIdAndFamilyIdAndRevokedAtIsNull(appId, familyId)
                .forEach(token -> token.revoke(by));
    }

    private Duration refreshTtl() {
        return jwtProperties.refreshTtl() != null ? jwtProperties.refreshTtl() : Duration.ofDays(30);
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record IssuedRefreshToken(String rawValue, String id, Instant expiresAt) { }

    public record RotationResult(String userId, String rawValue, Instant expiresAt) { }
}
