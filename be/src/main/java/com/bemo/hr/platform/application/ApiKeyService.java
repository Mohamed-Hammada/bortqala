package com.bemo.hr.platform.application;

import com.bemo.hr.platform.api.PlatformApi;
import com.bemo.hr.platform.domain.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
public class ApiKeyService {

    private static final int MAX_KEYS = 50;

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public PlatformApi.ApiKeyCreateResponse createKey(String appId, PlatformApi.ApiKeyCreateRequest request, String actor) {
        long count = apiKeyRepository.findByAppIdOrderByCreatedAtDesc(appId).size();
        if (count >= MAX_KEYS) {
            throw new BusinessRuleException("Maximum API keys limit reached", "APIKEY_LIMIT_REACHED", HttpStatus.CONFLICT);
        }
        String fullKey = generateFullKey();
        String hash = sha256(fullKey);
        String scopes = request.scopes() != null ? request.scopes() : "";
        int rateLimit = request.rateLimitPerMin() > 0 ? request.rateLimitPerMin() : 120;

        ApiKey apiKey = new ApiKey(appId, request.name(), hash, scopes, actor);
        apiKey.setRateLimitPerMin(rateLimit);
        apiKeyRepository.save(apiKey);

        return new PlatformApi.ApiKeyCreateResponse(
                apiKey.getId(), apiKey.getName(), fullKey, apiKey.getScopes(),
                apiKey.getRateLimitPerMin(), true, apiKey.getCreatedAt().toEpochMilli()
        );
    }

    public PlatformApi.ApiKeyListResponse listKeys(String appId) {
        List<PlatformApi.ApiKeyResponse> keys = apiKeyRepository.findByAppIdOrderByCreatedAtDesc(appId)
                .stream().map(this::toKeyResponse).toList();
        return new PlatformApi.ApiKeyListResponse(keys);
    }

    public void toggleKey(String appId, String keyId, boolean active) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .filter(k -> k.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("API key not found", "APIKEY_NOT_FOUND", HttpStatus.NOT_FOUND));
        key.setActive(active);
        apiKeyRepository.save(key);
    }

    public void revokeKey(String appId, String keyId) {
        toggleKey(appId, keyId, false);
    }

    public void deleteKey(String appId, String keyId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .filter(k -> k.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("API key not found", "APIKEY_NOT_FOUND", HttpStatus.NOT_FOUND));
        apiKeyRepository.delete(key);
    }

    private PlatformApi.ApiKeyResponse toKeyResponse(ApiKey key) {
        return new PlatformApi.ApiKeyResponse(
                key.getId(), key.getName(), key.getScopes(),
                key.getRateLimitPerMin(), key.isActive(),
                key.getLastUsedAt() != null ? key.getLastUsedAt().toEpochMilli() : null,
                key.getCreatedBy(),
                key.getCreatedAt().toEpochMilli(), key.getUpdatedAt().toEpochMilli(), key.getVersion()
        );
    }

    private static String generateFullKey() {
        byte[] random = new byte[32];
        new java.security.SecureRandom().nextBytes(random);
        return "bk_" + HexFormat.of().formatHex(random);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
