package com.bemo.hr.access.sso.application;

import com.bemo.hr.access.sso.domain.SsoConfig;
import com.bemo.hr.access.sso.domain.SsoConfigRepository;
import com.bemo.hr.access.sso.domain.UserSsoIdentity;
import com.bemo.hr.access.sso.domain.UserSsoIdentityRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SsoService {

    private final SsoConfigRepository ssoConfigRepository;
    private final UserSsoIdentityRepository userSsoIdentityRepository;
    private final com.bemo.hr.audit.application.AuditService auditService;

    public SsoConfig createConfig(SsoApi.CreateConfigRequest request) {
        String appId = TenantContext.require();
        SsoConfig config = new SsoConfig(
                appId,
                SsoConfig.Provider.valueOf(request.provider()),
                request.clientId(),
                request.clientSecret(),
                request.issuer(),
                request.discoveryUrl(),
                request.autoProvision(),
                request.defaultRole()
        );
        return ssoConfigRepository.save(config);
    }

    public SsoConfig updateConfig(String id, SsoApi.UpdateConfigRequest request) {
        SsoConfig config = findByIdOrThrow(id);
        if (request.clientId() != null) config.setClientId(request.clientId());
        if (request.clientSecret() != null) config.setSecret(request.clientSecret());
        if (request.issuer() != null) config.setIssuer(request.issuer());
        if (request.discoveryUrl() != null) config.setDiscoveryUrl(request.discoveryUrl());
        if (request.autoProvision() != null) config.setAutoProvision(request.autoProvision());
        if (request.defaultRole() != null) config.setDefaultRole(request.defaultRole());
        if (request.active() != null) config.setActive(request.active());
        return ssoConfigRepository.save(config);
    }

    public void deleteConfig(String id) {
        SsoConfig config = findByIdOrThrow(id);
        ssoConfigRepository.delete(config);
    }

    @Transactional(readOnly = true)
    public List<SsoConfig> listConfigs() {
        return ssoConfigRepository.findByAppIdAndActiveTrue(TenantContext.require());
    }

    @Transactional(readOnly = true)
    public SsoConfig getConfigById(String id) {
        return findByIdOrThrow(id);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveConfig(String provider) {
        String appId = TenantContext.require();
        return ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(appId, provider).isPresent();
    }

    @Transactional(readOnly = true)
    public SsoApi.StartResponse startAuth(String provider) {
        String appId = TenantContext.require();
        SsoConfig config = ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(appId, provider)
                .orElseThrow(() -> new NotFoundException("SSO config not found", "SSO_CONFIG_NOT_FOUND"));

        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        String stateToken = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (state + ":" + nonce + ":" + System.currentTimeMillis()).getBytes());

        String authorizationUrl = buildAuthorizationUrl(config, stateToken, nonce);

        return new SsoApi.StartResponse(authorizationUrl, stateToken);
    }

    @Transactional
    public SsoApi.CallbackResult handleCallback(String stateToken, String provider, String code) {
        String appId = TenantContext.require();
        validateStateToken(stateToken);

        SsoConfig config = ssoConfigRepository.findByAppIdAndProviderAndActiveTrue(appId, provider)
                .orElseThrow(() -> new BusinessRuleException("SSO provider is disabled", "SSO_PROVIDER_DISABLED", HttpStatus.FORBIDDEN));

        String subject = decodeSubjectFromCode(code);
        String email = subject + "@ssoplaceholder.com";
        String displayName = subject;

        var existingIdentity = userSsoIdentityRepository.findByAppIdAndProviderAndSubject(appId, provider, subject);
        if (existingIdentity.isPresent()) {
            return new SsoApi.CallbackResult(existingIdentity.get().getUserId(), false);
        }

        if (!config.isAutoProvision()) {
            throw new BusinessRuleException("SSO login not configured for this email", "SSO_UNKNOWN_EMAIL", HttpStatus.FORBIDDEN);
        }

        String userId = UUID.randomUUID().toString();
        UserSsoIdentity identity = new UserSsoIdentity(appId, userId, provider, subject, email, displayName);
        userSsoIdentityRepository.save(identity);

        log.info("Auto-provisioned SSO user {} for provider {}", userId, provider);
        return new SsoApi.CallbackResult(userId, true);
    }

    @Transactional(readOnly = true)
    public List<SsoApi.IdentityResponse> getUserIdentities(String userId) {
        String appId = TenantContext.require();
        return userSsoIdentityRepository.findByAppIdAndUserId(appId, userId).stream()
                .map(i -> new SsoApi.IdentityResponse(i.getId(), i.getProvider(), i.getEmail(), i.getDisplayName()))
                .toList();
    }

    private void validateStateToken(String stateToken) {
        if (stateToken == null || stateToken.isBlank()) {
            throw new BusinessRuleException("SSO state is invalid", "SSO_STATE_INVALID", HttpStatus.FORBIDDEN);
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(stateToken));
            String[] parts = decoded.split(":");
            if (parts.length < 3) throw new IllegalArgumentException();
            long timestamp = Long.parseLong(parts[2]);
            long fiveMinutesMs = 5 * 60 * 1000;
            if (System.currentTimeMillis() - timestamp > fiveMinutesMs) {
                throw new BusinessRuleException("SSO state has expired", "SSO_STATE_EXPIRED", HttpStatus.FORBIDDEN);
            }
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("SSO state is invalid", "SSO_STATE_INVALID", HttpStatus.FORBIDDEN);
        }
    }

    private String buildAuthorizationUrl(SsoConfig config, String state, String nonce) {
        String base = config.getDiscoveryUrl() != null ? config.getDiscoveryUrl() : config.getIssuer();
        if (base == null) base = "https://accounts.google.com";
        return base + "/o/oauth2/v2/auth?client_id=" + config.getClientId()
                + "&response_type=code&scope=openid+email+profile&state=" + state
                + "&nonce=" + nonce + "&redirect_uri=/api/v1/auth/sso/callback";
    }

    private String decodeSubjectFromCode(String code) {
        return "sso-user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private SsoConfig findByIdOrThrow(String id) {
        return ssoConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SSO config not found", "SSO_CONFIG_NOT_FOUND"));
    }
}
