package com.bemo.hr.shared.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            com.bemo.hr.shared.observability.RequestAuditFilter requestAuditFilter,
                                            CorsConfigurationSource corsConfigurationSource,
                                            AppUserRepository appUserRepository,
                                            ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(passwordChangeAwareAccessDeniedHandler(objectMapper)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/i18n/**",
                                "/api/v1/system/status",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/auth/change-password",
                                "/api/v1/auth/me"
                        ).authenticated()
                        .requestMatchers("/api/**").access(passwordChangeRestrictedRequestAuthorization())
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/assets/**",
                                "/icons/**",
                                "/*.js",
                                "/*.css",
                                "/*.json",
                                "/*.webmanifest",
                                "/*.svg",
                                "/*.png"
                        ).permitAll()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(appUserRepository))))
                .addFilterAfter(requestAuditFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthorizationManager<RequestAuthorizationContext> passwordChangeRestrictedRequestAuthorization() {
        return (supplier, context) -> {
            Authentication authentication = supplier.get();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return new AuthorizationDecision(false);
            }
            boolean restricted = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority()
                            .equals(PasswordChangeAwareAccessDeniedHandler.PASSWORD_CHANGE_AUTHORITY));
            return new AuthorizationDecision(!restricted);
        };
    }

    @Bean
    PasswordChangeAwareAccessDeniedHandler passwordChangeAwareAccessDeniedHandler(ObjectMapper objectMapper) {
        return new PasswordChangeAwareAccessDeniedHandler(objectMapper);
    }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository appUserRepository) {
        return principal -> {
            int separator = principal.indexOf('|');
            if (separator < 1 || separator == principal.length() - 1) {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Application is required.");
            }
            String appId = principal.substring(0, separator);
            String username = principal.substring(separator + 1);
            return appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username)
                .map(appUser -> User.withUsername(appUser.getUsername())
                        .password(appUser.getPasswordHash())
                        .disabled(!appUser.isActive())
                        .authorities(appUser.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode().name())).toList())
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found."));
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        validateSecret(properties.secret());
        var key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        validateSecret(properties.secret());
        var key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt,
            org.springframework.security.authentication.AbstractAuthenticationToken> jwtAuthenticationConverter(AppUserRepository appUserRepository) {
        return new RevocableJwtAuthenticationConverter(appUserRepository);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${hr.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") List<String> allowedOrigins) {
        for (String origin : allowedOrigins) {
            if (origin.equals("*") || origin.contains("*") || (!origin.startsWith("http://") && !origin.startsWith("https://"))) {
                throw new IllegalStateException("CORS origins must not contain wildcards and must have a valid scheme.");
            }
        }
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Device-Id", "X-Correlation-Id", "Cache-Control"));
        configuration.setExposedHeaders(List.of("Content-Disposition", "X-Correlation-Id", "X-Server-Correlation-Id"));
        configuration.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void validateSecret(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("HR_JWT_SECRET must contain at least 32 bytes.");
        }
    }
}
