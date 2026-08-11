package com.bemo.hr.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Demo-only "no-login superadmin" access. It is fully inert unless all of the
 * following hold:
 * <ul>
 *   <li>{@code hr.security.demo-no-login.enabled} is {@code true}, and</li>
 *   <li>{@code hr.security.demo-no-login.app-code} points at an existing app, and</li>
 *   <li>one of the active Spring profiles matches
 *       {@code hr.security.demo-no-login.profiles} (a comma-separated allow-list).</li>
 * </ul>
 * When active, a random secret is generated on every startup (unless
 * {@code HR_DEMO_SECRET} is provided) and logged once so a demo visitor can open
 * the dashboard as SUPER_ADMIN without a password.
 */
@Component
@Order(20)
public class DemoNoLoginService implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoNoLoginService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DemoNoLoginProperties properties;
    private final Environment environment;
    private final String secret;

    public DemoNoLoginService(DemoNoLoginProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
        this.secret = properties.secret() == null || properties.secret().isBlank()
                ? generateRandomSecret()
                : properties.secret();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isAvailable()) {
            if (properties.enabled()) {
                LOGGER.warn("hr.security.demo-no-login is enabled but the active profile does not match {} "
                        + "and/or the demo app code is missing; the no-login demo link stays disabled.",
                        properties.profiles());
            }
            return;
        }
        LOGGER.warn("===============================================================================");
        LOGGER.warn("DEMO NO-LOGIN SUPERADMIN ACCESS IS ENABLED (allowed profiles: {}).",
                properties.profiles());
        LOGGER.warn("Open the app URL with ?my_secret={} to enter the dashboard as SUPER_ADMIN "
                + "without a password.", secret);
        LOGGER.warn("The secret is regenerated on every application start unless HR_DEMO_SECRET is set.");
        LOGGER.warn("===============================================================================");
    }

    public boolean isAvailable() {
        if (!properties.enabled()) return false;
        if (properties.appCode() == null || properties.appCode().isBlank()) return false;
        return profileMatches();
    }

    public boolean isValidSecret(String candidate) {
        if (!isAvailable() || candidate == null || candidate.isBlank()) return false;
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
    }

    public String secret() {
        return secret;
    }

    private boolean profileMatches() {
        String allowed = properties.profiles();
        if (allowed == null || allowed.isBlank()) return true;
        Set<String> allowedSet = Arrays.stream(allowed.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (allowedSet.isEmpty()) return true;
        return Arrays.stream(environment.getActiveProfiles())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(allowedSet::contains);
    }

    private static String generateRandomSecret() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
