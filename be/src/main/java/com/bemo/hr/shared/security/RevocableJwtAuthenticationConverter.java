package com.bemo.hr.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;

public class RevocableJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AppUserRepository appUserRepository;
    private final JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    public RevocableJwtAuthenticationConverter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getSubject();
        String appId = jwt.getClaim("appId");
        Object rawTokenVersion = jwt.getClaim("tv");
        if (username == null || username.isBlank()) {
            throw new BadCredentialsException("Missing subject claim.");
        }
        if (appId == null || appId.isBlank()) {
            throw new BadCredentialsException("Missing appId claim.");
        }
        if (!(rawTokenVersion instanceof Number tokenVersion)) {
            throw new BadCredentialsException("Missing or invalid token version claim.");
        }
        boolean revoked = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username)
                .map(user -> !user.isActive() || user.getTokenVersion() != tokenVersion.intValue())
                .orElse(true);
        if (revoked) {
            throw new BadCredentialsException("Session has been revoked.");
        }
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
        if (Boolean.TRUE.equals(jwt.getClaim("pwc"))) {
            authorities = List.of(new SimpleGrantedAuthority(PasswordChangeAwareAccessDeniedHandler.PASSWORD_CHANGE_AUTHORITY));
        }
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
