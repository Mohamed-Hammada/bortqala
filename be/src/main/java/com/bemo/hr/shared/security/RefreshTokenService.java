package com.bemo.hr.shared.security;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
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

@Service
@Transactional
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public IssuedRefreshToken issue(String appId, String userId, String deviceId) {
        String raw = generateToken();
        Duration ttl = jwtProperties.refreshTtl() != null ? jwtProperties.refreshTtl() : Duration.ofDays(30);
        RefreshToken entity = new RefreshToken(appId, userId, hash(raw), Instant.now().plus(ttl), deviceId);
        refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(raw, entity.getId(), entity.getExpiresAt());
    }

    public String rotate(String appId, String rawToken, String deviceId) {
        RefreshToken existing = requireActive(appId, rawToken);
        String raw = generateToken();
        Duration ttl = jwtProperties.refreshTtl() != null ? jwtProperties.refreshTtl() : Duration.ofDays(30);
        RefreshToken replacement = new RefreshToken(appId, existing.getUserId(), hash(raw), Instant.now().plus(ttl), deviceId);
        refreshTokenRepository.save(replacement);
        existing.markReplacedBy(replacement.getId());
        return raw;
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
}
