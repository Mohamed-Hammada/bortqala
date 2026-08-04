package com.bemo.hr.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevocableJwtAuthenticationConverterTests {

    private AppUserRepository appUserRepository;
    private RevocableJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        converter = new RevocableJwtAuthenticationConverter(appUserRepository);
    }

    private Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ahmed")
                .claim("appId", "app-1")
                .claim("tv", 3);
    }

    private AppUser activeUser() {
        var user = mock(AppUser.class);
        when(user.isActive()).thenReturn(true);
        when(user.getTokenVersion()).thenReturn(3);
        Set<Role> roles = Set.of(mockRole(RoleCode.PAYROLL_MANAGER));
        when(user.getRoles()).thenReturn(roles);
        return user;
    }

    private Role mockRole(RoleCode code) {
        var role = mock(Role.class);
        when(role.getCode()).thenReturn(code);
        return role;
    }

    @Test
    void validTokenConvertsWithAuthoritiesFromRolesClaim() {
        var user = activeUser();
        when(appUserRepository.findByAppIdAndUsernameIgnoreCase("app-1", "ahmed"))
                .thenReturn(Optional.of(user));

        var result = converter.convert(baseJwt().claim("roles", "PAYROLL_MANAGER").build());

        assertThat(result).isInstanceOf(JwtAuthenticationToken.class);
        var authorities = result.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
        assertThat(authorities).contains("ROLE_PAYROLL_MANAGER");
    }

    @Test
    void missingSubjectIsRejected() {
        assertThatThrownBy(() -> converter.convert(baseJwt().subject(" ").build()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void missingAppIdIsRejected() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ahmed")
                .claim("tv", 3)
                .build();
        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("appId");
    }

    @Test
    void missingTokenVersionIsRejected() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ahmed")
                .claim("appId", "app-1")
                .build();
        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("token version");
    }

    @Test
    void inactiveUserIsRejected() {
        var user = activeUser();
        when(user.isActive()).thenReturn(false);
        when(appUserRepository.findByAppIdAndUsernameIgnoreCase("app-1", "ahmed"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> converter.convert(baseJwt().build()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void staleTokenVersionIsRejected() {
        var user = activeUser();
        when(user.getTokenVersion()).thenReturn(4);
        when(appUserRepository.findByAppIdAndUsernameIgnoreCase("app-1", "ahmed"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> converter.convert(baseJwt().build()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void unknownUserIsRejected() {
        when(appUserRepository.findByAppIdAndUsernameIgnoreCase("app-1", "ahmed"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> converter.convert(baseJwt().build()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");
    }
}
