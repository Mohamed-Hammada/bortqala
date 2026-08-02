package com.bemo.hr.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collection;

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
        Integer tokenVersion = jwt.getClaim("tv");
        if (tokenVersion != null) {
            String appId = jwt.getClaim("appId");
            String username = jwt.getSubject();
            boolean revoked = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username)
                    .map(user -> !user.isActive() || user.getTokenVersion() != tokenVersion)
                    .orElse(true);
            if (revoked) {
                throw new BadCredentialsException("Session has been revoked.");
            }
        }
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
