package com.bemo.hr.shared.security;

import com.bemo.hr.platform.domain.ApiKey;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication token for machine-to-machine calls authenticated with an {@code X-Api-Key} header.
 * <p>
 * Authorities are {@code ROLE_API_KEY} plus one {@code SCOPE_<scope>} authority per configured scope so that
 * {@code @auth.hasPermission('...')} SpEL guards can authorize API clients whose scopes mirror permission keys.
 */
public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final ApiKey apiKey;

    public ApiKeyAuthentication(ApiKey apiKey) {
        super(authoritiesFor(apiKey));
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    private static Set<SimpleGrantedAuthority> authoritiesFor(ApiKey apiKey) {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_API_KEY"));
        authorities.addAll(apiKey.scopeSet().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toSet()));
        if (apiKey.scopeSet().contains("*")) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_*"));
        }
        return authorities;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    @Override
    public Object getCredentials() {
        return apiKey.getKeyHash();
    }

    @Override
    public Object getPrincipal() {
        return apiKey;
    }
}