package com.bemo.hr.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final AuthService authService;
    private final String appCode;
    private final String appName;
    private final String adminUsername;
    private final String adminPassword;
    private final String superAdminUsername;
    private final String superAdminPassword;

    public BootstrapAdminInitializer(
            AuthService authService,
            @Value("${hr.bootstrap.app-code}") String appCode,
            @Value("${hr.bootstrap.app-name}") String appName,
            @Value("${hr.bootstrap.admin-username}") String adminUsername,
            @Value("${hr.bootstrap.admin-password}") String adminPassword,
            @Value("${hr.bootstrap.super-admin-username}") String superAdminUsername,
            @Value("${hr.bootstrap.super-admin-password}") String superAdminPassword) {
        this.authService = authService;
        this.appCode = appCode;
        this.appName = appName;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.superAdminUsername = superAdminUsername;
        this.superAdminPassword = superAdminPassword;
    }

    private static String required(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " is required for the mandatory administrator bootstrap.");
        }
        return value;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.ensureBootstrapAppAdmin(
                required(appCode, "hr.bootstrap.app-code"),
                required(appName, "hr.bootstrap.app-name"),
                required(superAdminUsername, "hr.bootstrap.super-admin-username"),
                required(superAdminPassword, "hr.bootstrap.super-admin-password"),
                "مدير النظام الشامل (Super Admin)",
                java.util.Set.of(RoleCode.SUPER_ADMIN, RoleCode.ADMIN));

        authService.ensureBootstrapAppAdmin(
                appCode,
                appName,
                required(adminUsername, "hr.bootstrap.admin-username"),
                required(adminPassword, "hr.bootstrap.admin-password"),
                "مدير التشغيل والنظام (Admin)",
                java.util.Set.of(RoleCode.ADMIN));
    }
}
