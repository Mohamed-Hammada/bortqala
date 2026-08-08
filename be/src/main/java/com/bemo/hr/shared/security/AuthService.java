package com.bemo.hr.shared.security;

import com.bemo.hr.access.application.AccessCatalogService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.workforce.WorkerCategoryRepository;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final UserPreferenceService userPreferenceService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;
    private final LoginStateService loginStateService;
    private final RefreshCookieCodec refreshCookieCodec;
    private final com.bemo.hr.audit.application.AuditService auditService;
    private final TranslationService translationService;
    private final WorkerCategoryRepository workerCategoryRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final TenantFeatureService tenantFeatureService;
    private final DemoNoLoginProperties demoNoLoginProperties;
    private final AccessCatalogService accessCatalogService;

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, JwtProperties jwtProperties,
                       AppUserRepository appUserRepository, RoleRepository roleRepository,
                       TenantApplicationRepository tenantApplicationRepository,
                       UserPreferenceService userPreferenceService, PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService, LoginRateLimiter loginRateLimiter,
                       LoginStateService loginStateService,
                       RefreshCookieCodec refreshCookieCodec,
                       com.bemo.hr.audit.application.AuditService auditService,
                       TranslationService translationService,
                       WorkerCategoryRepository workerCategoryRepository,
                       AttendanceCategoryRepository attendanceCategoryRepository,
                       TenantFeatureService tenantFeatureService,
                       DemoNoLoginProperties demoNoLoginProperties,
                       AccessCatalogService accessCatalogService) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.userPreferenceService = userPreferenceService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.loginRateLimiter = loginRateLimiter;
        this.loginStateService = loginStateService;
        this.refreshCookieCodec = refreshCookieCodec;
        this.auditService = auditService;
        this.translationService = translationService;
        this.workerCategoryRepository = workerCategoryRepository;
        this.attendanceCategoryRepository = attendanceCategoryRepository;
        this.tenantFeatureService = tenantFeatureService;
        this.demoNoLoginProperties = demoNoLoginProperties;
        this.accessCatalogService = accessCatalogService;
    }

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$XnjwgsuR3b/qd7/gd5SNmeewo2ZHV5Hw1Citn8vLnTcT9OaPckNYG";

    private static final String DEMO_SUPERADMIN_USERNAME = "demo_superadmin";

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LoginResult demoSuperadminLogin(String deviceId) {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue(demoNoLoginProperties.appCode())
                .orElseThrow(() -> new NotFoundException("The demo application is not configured.",
                        "DEMO_NO_LOGIN_APP_NOT_CONFIGURED"));
        String appId = app.getId();
        TenantContext.set(appId);
        try {
            AppUser user = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, DEMO_SUPERADMIN_USERNAME)
                    .orElseGet(() -> createDemoSuperadmin(appId));
            if (!user.isActive()) {
                throw new NotFoundException("The demo superadmin link is invalid or has expired.",
                        "DEMO_NO_LOGIN_LINK_INVALID");
            }
            Instant now = Instant.now();
            Instant accessExpiresAt = issueAccessToken(app, user, now);
            var refresh = refreshTokenService.issue(appId, user.getId(), deviceId);
            auditService.record("DEMO_SUPERADMIN_LOGIN", "USER", user.getId(), user.getUsername(),
                    "Entered the dashboard through the demo no-login link", null);
            return new LoginResult(appId,
                    new AuthApi.LoginResponse(accessToken(appId, user, now, accessExpiresAt), "Bearer", accessExpiresAt,
                            false,
                            new AuthApi.AppResponse(appId, app.getCode(), app.getName(),
                                    app.isAdminDashboardCustomizationEnabled()), toResponse(user),
                            toPreferenceResponse(preferenceFor(user), user, app)),
                    refresh.rawValue(), refresh.expiresAt());
        } finally {
            TenantContext.clear();
        }
    }

    private AppUser createDemoSuperadmin(String appId) {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        String randomPassword = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var roles = requireRoles(Set.of(RoleCode.SUPER_ADMIN));
        var created = new AppUser(appId, DEMO_SUPERADMIN_USERNAME, "Demo Super Admin",
                passwordEncoder.encode(randomPassword), roles, Set.of(), true, true);
        created.markPasswordChanged(Instant.now());
        return appUserRepository.save(created);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LoginResult login(AuthApi.LoginRequest request, String deviceId, String ip) {
        if (loginRateLimiter.isGlobalIpBlocked(ip)) {
            performDummyPasswordCheck(request.password());
            throw new BadCredentialsException("Invalid credentials.");
        }
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue(request.appCode());
        if (app.isEmpty()) {
            loginRateLimiter.recordGlobalIpFailure(ip);
            performDummyPasswordCheck(request.password());
            throw new BadCredentialsException("Invalid credentials.");
        }
        String appId = app.get().getId();
        TenantContext.set(appId);
        try {
            if (loginRateLimiter.isTenantBlocked(appId, request.username(), deviceId, ip)) {
                throw new BadCredentialsException("Invalid credentials.");
            }
            var userOpt = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, request.username());
            if (userOpt.isEmpty()) {
                loginRateLimiter.recordFailure(appId, request.username(), deviceId, ip);
                performDummyPasswordCheck(request.password());
                throw new BadCredentialsException("Invalid credentials.");
            }
            var user = userOpt.get();
            Instant now = Instant.now();
            if (user.isLocked(now)) {
                throw new BusinessRuleException("الحساب مقفل مؤقتاً بسبب محاولات تسجيل دخول خاطئة متكررة.",
                        "ACCOUNT_TEMPORARILY_LOCKED", HttpStatus.UNAUTHORIZED);
            }
            try {
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                        appId + "|" + request.username(), request.password()));
            } catch (BadCredentialsException exception) {
                loginRateLimiter.recordFailure(appId, request.username(), deviceId, ip);
                loginStateService.recordFailure(appId, request.username(), now);
                throw exception;
            }
            loginStateService.recordSuccess(appId, request.username(), now);
            loginRateLimiter.reset(appId, request.username());
            Instant accessExpiresAt = issueAccessToken(app.get(), user, now);
            var refresh = refreshTokenService.issue(appId, user.getId(), deviceId);
            auditService.record("USER_LOGIN", "USER", user.getId(), user.getUsername(), "Successful login", null);
            return new LoginResult(appId,
                    new AuthApi.LoginResponse(accessToken(appId, user, now, accessExpiresAt), "Bearer", accessExpiresAt,
                            user.isMustChangePassword(),
                            new AuthApi.AppResponse(appId, app.get().getCode(), app.get().getName(),
                                    app.get().isAdminDashboardCustomizationEnabled()), toResponse(user),
                            toPreferenceResponse(preferenceFor(user), user, app.get())),
                    refresh.rawValue(), refresh.expiresAt());
        } finally {
            TenantContext.clear();
        }
    }

    private void performDummyPasswordCheck(String password) {
        passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RefreshResult refresh(String cookieValue, String deviceId) {
        RefreshCookieCodec.Decoded decoded = refreshCookieCodec.decode(cookieValue);
        String appId = decoded.appId();
        TenantContext.set(appId);
        try {
            var rotation = refreshTokenService.rotate(appId, decoded.rawToken(), deviceId, "refresh");
            var user = appUserRepository.findByAppIdAndId(appId, rotation.userId())
                    .orElseThrow(() -> new BusinessRuleException("Session is invalid or expired.",
                            "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED));
            if (!user.isActive()) {
                refreshTokenService.revoke(appId, rotation.rawValue(), "refresh");
                throw new BusinessRuleException("Session is invalid or expired.",
                        "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
            }
            Instant now = Instant.now();
            Instant accessExpiresAt = issueAccessToken(appId, user, now);
            return new RefreshResult(appId,
                    new AuthApi.RefreshResponse(accessToken(appId, user, now, accessExpiresAt), "Bearer", accessExpiresAt),
                    rotation.rawValue(), rotation.expiresAt());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void logout(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return;
        RefreshCookieCodec.Decoded decoded = refreshCookieCodec.decode(cookieValue);
        TenantContext.set(decoded.appId());
        try {
            refreshTokenService.revoke(decoded.appId(), decoded.rawToken(),
                    refreshTokenService.usernameFor(decoded.appId(), decoded.rawToken()));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void changePassword(String username, AuthApi.ChangePasswordRequest request) {
        var app = requireCurrentApp();
        var user = requireByUsername(app.getId(), username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("كلمة المرور الحالية غير صحيحة.", "PASSWORD_MISMATCH", HttpStatus.BAD_REQUEST);
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessRuleException("كلمة المرور الجديدة يجب أن تختلف عن كلمة المرور الحالية.",
                    "PASSWORD_REUSE", HttpStatus.BAD_REQUEST);
        }
        validatePasswordStrength(request.newPassword(), app);
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        user.markPasswordChanged(Instant.now());
        refreshTokenService.revokeAllForUser(app.getId(), user.getId(), username);
        auditService.record("PASSWORD_CHANGE", "USER", user.getId(), username, "Changed password and revoked all sessions", null);
    }

    @Transactional
    public void revokeSessions(String id, String currentUsername) {
        String appId = TenantContext.require();
        var user = appUserRepository.findById(id).filter(item -> item.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("User not found.", "AUTH_USER_NOT_FOUND"));
        user.bumpTokenVersion();
        refreshTokenService.revokeAllForUser(appId, user.getId(), currentUsername);
        auditService.record("SESSIONS_REVOKED", "USER", user.getId(), currentUsername,
                "Revoked all sessions for user " + user.getUsername(), null);
    }

    @Transactional
    public void unlock(String id, String currentUsername) {
        String appId = TenantContext.require();
        var user = appUserRepository.findById(id).filter(item -> item.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("User not found.", "AUTH_USER_NOT_FOUND"));
        user.unlock();
        auditService.record("USER_UNLOCKED", "USER", user.getId(), currentUsername,
                "Unlocked account for user " + user.getUsername(), null);
    }

    public AuthApi.UserResponse current(String username) {
        return toResponse(requireByUsername(TenantContext.require(), username));
    }

    @Transactional
    public AuthApi.MeResponse me(String username, Instant sessionExpiresAt) {
        var app = requireCurrentApp();
        var user = requireByUsername(app.getId(), username);
        return new AuthApi.MeResponse(
                user.getId(), user.getUsername(), user.getDisplayName(),
                new AuthApi.TenantInfo(app.getId(), app.getCode(), app.getName()),
                user.getRoles().stream().map(Role::getCode).collect(Collectors.toUnmodifiableSet()),
                user.getRoles().stream().map(role -> role.getCode().name()).sorted().collect(Collectors.toUnmodifiableSet()),
                user.getAllowedMenus(), menuAccessMode(user),
                user.isCanViewSalary(), user.getCategoryId(),
                dashboardLayoutAllowed(user, app), user.isActive(),
                new AuthApi.SessionInfo(sessionExpiresAt, app.getSessionTimeoutMinutes(), app.isSessionTimeoutEnabled()),
                user.getVersion());
    }

    @Transactional
    public AuthApi.PreferenceResponse currentPreferences(String username) {
        var app = requireCurrentApp();
        var user = requireByUsername(app.getId(), username);
        return toPreferenceResponse(preferenceFor(user), user, app);
    }

    @Transactional
    public AuthApi.PreferenceResponse updatePreferences(String username, AuthApi.PreferenceRequest request) {
        var app = requireCurrentApp();
        var user = requireByUsername(app.getId(), username);
        return toPreferenceResponse(userPreferenceService.update(user.getId(), request), user, app);
    }

    @Transactional
    public AuthApi.PreferenceResponse updateNavigationPreferences(String username, AuthApi.NavigationPreferenceRequest request) {
        var app = requireCurrentApp();
        var user = requireByUsername(app.getId(), username);
        return toPreferenceResponse(userPreferenceService.updateNavigation(user.getId(), request), user, app);
    }

    @Transactional
    public AuthApi.PreferenceResponse updateDashboardPreferences(String username, AuthApi.DashboardPreferenceRequest request) {
        var app = requireCurrentApp();
        var user = requireByUsername(app.getId(), username);
        boolean layoutAllowed = dashboardLayoutAllowed(user, app);
        var result = userPreferenceService.updateDashboard(user.getId(), request, layoutAllowed);
        auditService.record("DASHBOARD_PREFERENCES_UPDATE", "USER_PREFERENCE", user.getId(), username,
                "Updated dashboard preferences; layoutAllowed=" + layoutAllowed, null);
        return toPreferenceResponse(result, user, app);
    }

    public java.util.List<AuthApi.UserResponse> listUsers() {
        return appUserRepository.findAllByAppIdOrderByDisplayNameAsc(TenantContext.require()).stream()
                .map(this::toResponse).toList();
    }

    public List<AuthApi.UserCategoryResponse> listCategories() {
        var employeeScopes = java.util.List.of(com.bemo.hr.employee.domain.CategoryScope.EMPLOYEE,
                com.bemo.hr.employee.domain.CategoryScope.BOTH);
        var workerScopes = java.util.List.of(com.bemo.hr.employee.domain.CategoryScope.WORKER,
                com.bemo.hr.employee.domain.CategoryScope.BOTH);
        var canonicalById = new java.util.HashMap<String, com.bemo.hr.employee.domain.AttendanceCategory>();
        attendanceCategoryRepository.findByScopeIn(employeeScopes).stream()
                .filter(com.bemo.hr.employee.domain.AttendanceCategory::isActive)
                .forEach(c -> canonicalById.putIfAbsent(c.getId(), c));
        workerCategoryRepository.findByStatus("ACTIVE").stream()
                .map(com.bemo.hr.workforce.WorkerCategory::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .forEach(categoryId -> attendanceCategoryRepository.findById(categoryId)
                        .filter(c -> c.isActive() && workerScopes.contains(c.getScope()))
                        .ifPresent(c -> canonicalById.putIfAbsent(c.getId(), c)));
        return canonicalById.values().stream()
                .map(c -> new AuthApi.UserCategoryResponse(c.getId(), c.getCode(), c.getName(), c.getScope().name()))
                .sorted(java.util.Comparator.comparing(AuthApi.UserCategoryResponse::name)).toList();
    }

    public AuthApi.AppSettingsResponse currentAppSettings() {
        return toSettingsResponse(requireCurrentApp());
    }

    @Transactional
    public AuthApi.AppSettingsResponse updateAppSettings(AuthApi.AppSettingsRequest request, String currentUsername) {
        var app = requireCurrentApp();
        var actor = requireByUsername(app.getId(), currentUsername);
        if (request.adminDashboardCustomizationEnabled() != app.isAdminDashboardCustomizationEnabled()
                && !hasRole(actor, RoleCode.SUPER_ADMIN)) {
            throw new BusinessRuleException("Only a Super Admin can change dashboard customization access for admins.",
                    "AUTH_DASHBOARD_CUSTOMIZATION_SUPER_ADMIN_ONLY", HttpStatus.CONFLICT);
        }
        int minPass = request.minPasswordLength() == null || request.minPasswordLength() <= 0 ? 8 : request.minPasswordLength();
        app.updateSettings(request.sessionTimeoutMinutes(), request.sessionTimeoutEnabled(), request.showReportPresets(), minPass);
        app.updateAttendanceAnomalyThreshold(request.attendanceAnomalyThresholdPercent());
        app.updateProcurementNumbering(request.automaticProcurementNumbering());
        app.updateDocumentNumbering(request.automaticDocumentNumbering());
        if (hasRole(actor, RoleCode.SUPER_ADMIN)) {
            app.updateDashboardPolicy(request.adminDashboardCustomizationEnabled());
        }
        int maxPass = request.maxPasswordLength() == null || request.maxPasswordLength() <= 0 ? 128 : request.maxPasswordLength();
        int expiry = request.passwordExpiryDays() == null ? 0 : request.passwordExpiryDays();
        int history = request.passwordHistoryCount() == null ? 0 : request.passwordHistoryCount();
        app.updatePasswordPolicy(
                minPass,
                request.requireUppercase(),
                request.requireLowercase(),
                request.requireNumbers(),
                request.requireSpecialChars(),
                request.disallowSpaces(),
                maxPass,
                expiry,
                history
        );
        auditService.record("SETTINGS_UPDATE", "TENANT_APPLICATION", app.getId(), currentUsername,
                "Updated tenant settings, security policy, and dashboard policy", null);
        return toSettingsResponse(app);
    }

    @Transactional
    public AuthApi.UserResponse create(AuthApi.UserUpsertRequest request, String currentUsername) {
        String appId = TenantContext.require();
        validate(request, appId, null, true);
        var actor = requireByUsername(appId, currentUsername);
        var actorRoles = actor.getRoles().stream().map(Role::getCode).map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        var menuCodes = request.allowedMenus() == null ? java.util.List.<String>of() : request.allowedMenus().stream().toList();
        accessCatalogService.validateAssignmentOrThrow(actorRoles, actor.getId(), request.roles().stream().map(Enum::name).toList(),
                menuCodes, null, null, request.accessChangeReason());
        var user = new AppUser(appId, request.username(), request.displayName(), passwordEncoder.encode(request.password()),
                requireRoles(request.roles()), request.allowedMenus(), request.canViewSalary(),
                request.dashboardCustomizationEnabled());
        user.requirePasswordChangeOnNextLogin();
        validateCategory(request.categoryId());
        user.assignCategory(request.categoryId());
        appUserRepository.save(user);
        auditService.record("USER_CREATE", "USER", user.getId(), currentUsername,
                "Created user " + user.getDisplayName() + " roles=" + request.roles()
                        + (request.accessChangeReason() == null || request.accessChangeReason().isBlank()
                        ? "" : " accessChangeReason=" + request.accessChangeReason().strip()), null);
        return toResponse(user);
    }

    @Transactional
    public AuthApi.UserResponse update(String id, AuthApi.UserUpsertRequest request, String currentUsername) {
        String appId = TenantContext.require();
        var user = appUserRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new NotFoundException("User not found.", "AUTH_USER_NOT_FOUND"));
        if (request.version() == null || request.version() != user.getVersion()) {
            throw new BusinessRuleException("This user changed since it was loaded. Refresh and try again.",
                    "AUTH_USER_VERSION_CONFLICT", HttpStatus.CONFLICT);
        }

        var actor = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, currentUsername)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Current user not found."));
        boolean actorIsSuperAdmin = actor.getRoles().stream()
                .anyMatch(role -> role.getCode() == RoleCode.SUPER_ADMIN);
        boolean targetIsSuperAdmin = user.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.SUPER_ADMIN);

        if (!actorIsSuperAdmin) {
            if (targetIsSuperAdmin) {
                throw new BusinessRuleException("Only a Super Admin can modify or deactivate Super Admin accounts.",
                        "AUTH_SUPER_ADMIN_ACCOUNT_PROTECTED", HttpStatus.CONFLICT);
            }
            if (request.roles().contains(RoleCode.SUPER_ADMIN)) {
                throw new BusinessRuleException("Only a Super Admin can assign the Super Admin role.",
                        "AUTH_SUPER_ADMIN_ROLE_ASSIGNMENT_FORBIDDEN", HttpStatus.CONFLICT);
            }
        }

        boolean targetCurrentlyActiveAdmin = user.isActive() && isAdminUser(user);
        boolean targetWillBeActiveAdmin = request.active() && isAdminRoles(request.roles());
        if (targetCurrentlyActiveAdmin && !targetWillBeActiveAdmin && activeAdminCount(appId) <= 1) {
            throw new BusinessRuleException("لا يمكن تعطيل أو إزالة دور آخر مسؤول نشط في النظام.",
                    "FINAL_ADMIN_PROTECTION", HttpStatus.BAD_REQUEST);
        }

        validate(request, appId, id, false);
        if (user.getUsername().equalsIgnoreCase(currentUsername) && !request.active()) {
            throw new BusinessRuleException("You cannot deactivate your own account.",
                    "AUTH_SELF_DEACTIVATE_FORBIDDEN", HttpStatus.CONFLICT);
        }
        var previousRoles = user.getRoles().stream().map(Role::getCode).map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        var actorRoles = actor.getRoles().stream().map(Role::getCode).map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        var menuCodes = request.allowedMenus() == null ? java.util.List.<String>of() : request.allowedMenus().stream().toList();
        accessCatalogService.validateAssignmentOrThrow(actorRoles, actor.getId(),
                request.roles().stream().map(Enum::name).toList(), menuCodes, id, previousRoles,
                request.accessChangeReason());

        String passwordHash = request.password() == null || request.password().isBlank()
                ? null : passwordEncoder.encode(request.password());
        user.update(request.username(), request.displayName(), passwordHash, request.active(),
                requireRoles(request.roles()), request.allowedMenus(), request.canViewSalary(),
                request.dashboardCustomizationEnabled());
        if (passwordHash != null) {
            user.requirePasswordChangeOnNextLogin();
            user.bumpTokenVersion();
            refreshTokenService.revokeAllForUser(appId, user.getId(), currentUsername);
        }
        validateCategory(request.categoryId());
        user.assignCategory(request.categoryId());
        auditService.record("USER_UPDATE", "USER", user.getId(), currentUsername,
                accessChangeDetails(user, request, previousRoles), null);
        return toResponse(user);
    }

    private String accessChangeDetails(AppUser user, AuthApi.UserUpsertRequest request, Set<String> previousRoles) {
        var newRoles = request.roles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
        var added = new java.util.TreeSet<>(newRoles);
        added.removeAll(previousRoles);
        var removed = new java.util.TreeSet<>(previousRoles);
        removed.removeAll(newRoles);
        return "Updated user " + user.getUsername() + " active=" + user.isActive()
                + " previousRoles=" + previousRoles + " newRoles=" + newRoles
                + " added=" + added + " removed=" + removed
                + (request.accessChangeReason() == null || request.accessChangeReason().isBlank()
                ? "" : " accessChangeReason=" + request.accessChangeReason().strip());
    }

    @Transactional
    public AppUser ensureBootstrapAppAdmin(String appCode, String appName, String username, String password) {
        return ensureBootstrapAppAdmin(appCode, appName, username, password, "مدير التشغيل والنظام (Admin)", Set.of(RoleCode.ADMIN));
    }

    @Transactional
    public AppUser ensureBootstrapAppAdmin(String appCode, String appName, String username, String password, String displayName, Set<RoleCode> roleCodes) {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue(appCode)
                .orElseGet(() -> tenantApplicationRepository.save(new TenantApplication(appCode, appName)));
        return appUserRepository.findByAppIdAndUsernameIgnoreCase(app.getId(), username).orElseGet(() -> {
            var roles = requireRoles(roleCodes);
            var created = appUserRepository.save(new AppUser(app.getId(), username, displayName,
                    passwordEncoder.encode(password), roles, Set.of("dashboard","categories","employees","imports","parties","reports","operations","payroll","users","settings","workforce-dashboard","workforce-contractors","workforce-workers","workforce-categories","workforce-requests","workforce-attendance","workforce-settlements","workforce-advances","workforce-accounts","workforce-reports","approvals-my-tasks","approvals-workflows","budgets"), true, true));
            created.requirePasswordChangeOnNextLogin();
            return created;
        });
    }

    private Instant issueAccessToken(TenantApplication app, AppUser user, Instant now) {
        return issueAccessToken(app.getId(), user, now);
    }

    private Instant issueAccessToken(String appId, AppUser user, Instant now) {
        var app = tenantApplicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Application not found.", "APP_NOT_FOUND"));
        Duration ttl = jwtProperties.ttl();
        if (app.isSessionTimeoutEnabled() && app.getSessionTimeoutMinutes() > 0) {
            ttl = Duration.ofMinutes(app.getSessionTimeoutMinutes());
        }
        return now.plus(ttl);
    }

    private String accessToken(String appId, AppUser user, Instant now, Instant expiresAt) {
        var app = tenantApplicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Application not found.", "APP_NOT_FOUND"));
        boolean passwordChangeRequired = user.isMustChangePassword();
        var builder = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("appId", app.getId())
                .claim("appCode", app.getCode())
                .claim("name", user.getDisplayName())
                .claim("tv", user.getTokenVersion())
                .claim("pwc", passwordChangeRequired)
                .claim("roles", passwordChangeRequired
                        ? List.of()
                        : user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList());
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), builder.build())).getTokenValue();
    }

    private boolean isAdminUser(AppUser user) {
        return user.getRoles().stream().anyMatch(role -> role.getCode() == RoleCode.ADMIN
                || role.getCode() == RoleCode.SUPER_ADMIN);
    }

    private boolean isAdminRoles(Set<RoleCode> roles) {
        return roles.contains(RoleCode.ADMIN) || roles.contains(RoleCode.SUPER_ADMIN);
    }

    private long activeAdminCount(String appId) {
        return appUserRepository.lockAllByAppIdOrderByDisplayNameAsc(appId).stream()
                .filter(AppUser::isActive)
                .filter(this::isAdminUser)
                .count();
    }

    private void validate(AuthApi.UserUpsertRequest request, String appId, String currentId, boolean passwordRequired) {
        if (request.roles() == null || request.roles().isEmpty()) throw new BusinessRuleException("Select at least one role.",
                "AUTH_ROLE_REQUIRED", HttpStatus.CONFLICT);
        var app = requireCurrentApp();
        if (passwordRequired && (request.password() == null || request.password().isBlank())) {
            throw new BusinessRuleException("Password is required for a new user.",
                    "AUTH_PASSWORD_REQUIRED", HttpStatus.CONFLICT);
        }
        if (request.password() != null && !request.password().isBlank()) {
            validatePasswordStrength(request.password(), app);
        }
        boolean duplicate = currentId == null
                ? appUserRepository.existsByAppIdAndUsernameIgnoreCase(appId, request.username())
                : appUserRepository.existsByAppIdAndUsernameIgnoreCaseAndIdNot(appId, request.username(), currentId);
        if (duplicate) throw new BusinessRuleException("Username already exists.",
                "AUTH_USERNAME_EXISTS", HttpStatus.CONFLICT);
    }

    private void validatePasswordStrength(String password, TenantApplication app) {
        int minLen = app.getMinPasswordLength() > 0 ? app.getMinPasswordLength() : 8;
        int maxLen = app.getMaxPasswordLength() > 0 ? app.getMaxPasswordLength() : 128;

        if (password.length() < minLen) {
            throw new BusinessRuleException("يجب أن لا تقل كلمة المرور عن " + minLen + " أحرف.");
        }
        if (password.length() > maxLen) {
            throw new BusinessRuleException("يجب أن لا تتجاوز كلمة المرور " + maxLen + " حرفاً.");
        }
        if (app.isDisallowSpaces() && password.contains(" ")) {
            throw new BusinessRuleException("كلمة المرور يجب أن لا تحتوي على مسافات.",
                    "PASSWORD_NO_SPACES", HttpStatus.CONFLICT);
        }
        if (app.isRequireUppercase() && !password.chars().anyMatch(Character::isUpperCase)) {
            throw new BusinessRuleException("كلمة المرور يجب أن تحتوي على حرف كبير على الأقل.",
                    "PASSWORD_REQUIRES_UPPERCASE", HttpStatus.CONFLICT);
        }
        if (app.isRequireLowercase() && !password.chars().anyMatch(Character::isLowerCase)) {
            throw new BusinessRuleException("كلمة المرور يجب أن تحتوي على حرف صغير على الأقل.",
                    "PASSWORD_REQUIRES_LOWERCASE", HttpStatus.CONFLICT);
        }
        if (app.isRequireNumbers() && !password.chars().anyMatch(Character::isDigit)) {
            throw new BusinessRuleException("كلمة المرور يجب أن تحتوي على رقم واحد على الأقل.",
                    "PASSWORD_REQUIRES_DIGIT", HttpStatus.CONFLICT);
        }
        if (app.isRequireSpecialChars() && !password.matches(".*[^A-Za-z0-9].*")) {
            throw new BusinessRuleException("كلمة المرور يجب أن تحتوي على رمز خاص واحد على الأقل.",
                    "PASSWORD_REQUIRES_SPECIAL", HttpStatus.CONFLICT);
        }
    }

    private Set<Role> requireRoles(Set<RoleCode> codes) {
        var roles = roleRepository.findAllById(codes);
        if (roles.size() != codes.size()) {
            for (RoleCode code : codes) {
                if (!roleRepository.existsById(code)) {
                    roleRepository.save(new Role(code, code.name().replace('_', ' ')));
                }
            }
            roles = roleRepository.findAllById(codes);
        }
        return Set.copyOf(roles);
    }

    private AppUser requireByUsername(String appId, String username) {
        return appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username)
                .orElseThrow(() -> new NotFoundException("User not found.", "AUTH_USER_NOT_FOUND"));
    }

    private AuthApi.UserResponse toResponse(AppUser user) {
        var activeFeatures = tenantFeatureService.getAllEnabled(user.getAppId());
        return new AuthApi.UserResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRoles().stream().map(Role::getCode).collect(Collectors.toUnmodifiableSet()),
                user.getAllowedMenus(), menuAccessMode(user), user.isCanViewSalary(), user.getCategoryId(),
                user.isDashboardCustomizationEnabled(), user.isActive(), user.getVersion(), activeFeatures);
    }

    private String menuAccessMode(AppUser user) {
        return user.isMenuAccessAll() ? "ALL" : "SELECTED";
    }

    private boolean hasRole(AppUser user, RoleCode roleCode) {
        return user.getRoles().stream().anyMatch(role -> role.getCode() == roleCode);
    }

    private void validateCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) return;
        boolean exists = attendanceCategoryRepository.findById(categoryId).filter(c -> c.isActive()).isPresent();
        if (!exists) throw new BusinessRuleException("Select an active category.",
                "AUTH_ACTIVE_CATEGORY_REQUIRED", HttpStatus.CONFLICT);
    }

    private UserPreference preferenceFor(AppUser user) {
        return userPreferenceService.currentOrCreate(user.getId());
    }

    private AuthApi.PreferenceResponse toPreferenceResponse(UserPreference preference, AppUser user, TenantApplication app) {
        return new AuthApi.PreferenceResponse(preference.getTheme(), preference.getTableDensity(),
                preference.getLocale(), preference.getExcelTableStyle(), preference.getDefaultPageSize(),
                preference.getDefaultPage(), preference.isShowFavorites(), preference.isShowRecentlyUsed(),
                preference.getMaxRecentlyUsed(), preference.favoriteMenuIds(), preference.recentMenuIds(),
                preference.dashboardWidgetIds(), preference.isDashboardAnimationsEnabled(),
                dashboardLayoutAllowed(user, app),
                preference.getUpdatedAt());
    }

    private boolean dashboardLayoutAllowed(AppUser user, TenantApplication app) {
        if (hasRole(user, RoleCode.SUPER_ADMIN)) return true;
        if (!user.isDashboardCustomizationEnabled()) return false;
        return !hasRole(user, RoleCode.ADMIN) || app.isAdminDashboardCustomizationEnabled();
    }

    private TenantApplication requireCurrentApp() {
        String appId = TenantContext.require();
        return tenantApplicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Application not found.", "APP_NOT_FOUND"));
    }

    private AuthApi.AppSettingsResponse toSettingsResponse(TenantApplication app) {
        return new AuthApi.AppSettingsResponse(
                app.getSessionTimeoutMinutes(),
                app.isSessionTimeoutEnabled(),
                app.isShowReportPresets(),
                app.getAttendanceAnomalyThresholdPercent(),
                app.isAutomaticProcurementNumbering(),
                app.isAutomaticDocumentNumbering(),
                app.isAdminDashboardCustomizationEnabled(),
                app.getMinPasswordLength(),
                app.isRequireUppercase(),
                app.isRequireLowercase(),
                app.isRequireNumbers(),
                app.isRequireSpecialChars(),
                app.isDisallowSpaces(),
                app.getMaxPasswordLength(),
                app.getPasswordExpiryDays(),
                app.getPasswordHistoryCount(),
                app.getUpdatedAt()
        );
    }

    public record LoginResult(String appId, AuthApi.LoginResponse response, String refreshToken, Instant refreshExpiresAt) { }

    public record RefreshResult(String appId, AuthApi.RefreshResponse response, String refreshToken, Instant refreshExpiresAt) { }
}
