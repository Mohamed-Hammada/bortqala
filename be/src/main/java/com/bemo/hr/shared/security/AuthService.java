package com.bemo.hr.shared.security;

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
    static final int MAX_LOGIN_ATTEMPTS = 5;
    static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

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
    private final com.bemo.hr.audit.application.AuditService auditService;
    private final TranslationService translationService;
    private final WorkerCategoryRepository workerCategoryRepository;
    private final AttendanceCategoryRepository attendanceCategoryRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, JwtProperties jwtProperties,
                       AppUserRepository appUserRepository, RoleRepository roleRepository,
                       TenantApplicationRepository tenantApplicationRepository,
                       UserPreferenceService userPreferenceService, PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService, LoginRateLimiter loginRateLimiter,
                       com.bemo.hr.audit.application.AuditService auditService,
                       TranslationService translationService,
                       WorkerCategoryRepository workerCategoryRepository,
                       AttendanceCategoryRepository attendanceCategoryRepository) {
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
        this.auditService = auditService;
        this.translationService = translationService;
        this.workerCategoryRepository = workerCategoryRepository;
        this.attendanceCategoryRepository = attendanceCategoryRepository;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LoginResult login(AuthApi.LoginRequest request, String deviceId, String ip) {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue(request.appCode())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials."));
        TenantContext.set(app.getId());
        try {
            if (!loginRateLimiter.allow(app.getId(), request.username(), deviceId, ip)) {
                throw new BadCredentialsException("Invalid credentials.");
            }
            var userOpt = appUserRepository.findByAppIdAndUsernameIgnoreCase(app.getId(), request.username());
            if (userOpt.isEmpty()) {
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
                        app.getId() + "|" + request.username(), request.password()));
            } catch (BadCredentialsException exception) {
                recordFailedLogin(app.getId(), request.username(), now);
                throw exception;
            }
            user.recordSuccessfulLogin(now);
            loginRateLimiter.reset(app.getId(), request.username());
            Instant accessExpiresAt = issueAccessToken(app, user, now);
            var refresh = refreshTokenService.issue(app.getId(), user.getId(), deviceId);
            auditService.record("USER_LOGIN", "USER", user.getId(), user.getUsername(), "Successful login", null);
            return new LoginResult(
                    new AuthApi.LoginResponse(accessToken(app.getId(), user, now, accessExpiresAt), "Bearer", accessExpiresAt,
                            user.isMustChangePassword(),
                            new AuthApi.AppResponse(app.getId(), app.getCode(), app.getName(),
                                    app.isAdminDashboardCustomizationEnabled()), toResponse(user),
                            toPreferenceResponse(preferenceFor(user), user, app)),
                    refresh.rawValue(), refresh.expiresAt());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RefreshResult refresh(String cookieValue, String deviceId) {
        if (cookieValue == null || cookieValue.isBlank()) {
            throw new BusinessRuleException("Session is invalid or expired.",
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        String appId = cookieAppId(cookieValue);
        String rawToken = cookieRawToken(cookieValue);
        TenantContext.set(appId);
        try {
            var refreshToken = refreshTokenService.requireActive(appId, rawToken);
            var user = appUserRepository.findByAppIdAndId(appId, refreshToken.getUserId())
                    .orElseThrow(() -> new BusinessRuleException("Session is invalid or expired.",
                            "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED));
            if (!user.isActive() || user.isMustChangePassword()) {
                throw new BusinessRuleException("Session is invalid or expired.",
                        "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
            }
            Instant now = Instant.now();
            Instant accessExpiresAt = issueAccessToken(appId, user, now);
            String nextRaw = refreshTokenService.rotate(appId, rawToken, deviceId);
            var next = refreshTokenService.requireActive(appId, nextRaw);
            auditService.record("TOKEN_REFRESH", "USER", user.getId(), user.getUsername(), "Refreshed session token", null);
            return new RefreshResult(
                    new AuthApi.RefreshResponse(accessToken(appId, user, now, accessExpiresAt), "Bearer", accessExpiresAt),
                    nextRaw, next.getExpiresAt());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void logout(String cookieValue, String username) {
        if (cookieValue == null || cookieValue.isBlank()) return;
        String appId = cookieAppId(cookieValue);
        String rawToken = cookieRawToken(cookieValue);
        TenantContext.set(appId);
        try {
            refreshTokenService.revoke(appId, rawToken, username);
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
                .orElseThrow(() -> new NotFoundException("User not found."));
        user.bumpTokenVersion();
        refreshTokenService.revokeAllForUser(appId, user.getId(), currentUsername);
        auditService.record("SESSIONS_REVOKED", "USER", user.getId(), currentUsername,
                "Revoked all sessions for user " + user.getUsername(), null);
    }

    @Transactional
    public void unlock(String id, String currentUsername) {
        String appId = TenantContext.require();
        var user = appUserRepository.findById(id).filter(item -> item.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("User not found."));
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
        var categories = new java.util.ArrayList<AuthApi.UserCategoryResponse>();
        attendanceCategoryRepository.findAllByOrderByNameAsc().stream().filter(c -> c.isActive())
                .map(c -> new AuthApi.UserCategoryResponse(c.getId(), c.getCode(), c.getName(), "EMPLOYEE"))
                .forEach(categories::add);
        workerCategoryRepository.findByStatus("ACTIVE").stream()
                .map(c -> new AuthApi.UserCategoryResponse(c.getId(), c.getCode(), c.getName(), "WORKER"))
                .forEach(categories::add);
        return categories.stream().sorted(java.util.Comparator.comparing(AuthApi.UserCategoryResponse::name)).toList();
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
            throw new BusinessRuleException("Only a Super Admin can change dashboard customization access for admins.");
        }
        int minPass = request.minPasswordLength() == null || request.minPasswordLength() <= 0 ? 8 : request.minPasswordLength();
        app.updateSettings(request.sessionTimeoutMinutes(), request.sessionTimeoutEnabled(), request.showReportPresets(), minPass);
        app.updateAttendanceAnomalyThreshold(request.attendanceAnomalyThresholdPercent());
        app.updateProcurementNumbering(request.automaticProcurementNumbering());
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
    public AuthApi.UserResponse create(AuthApi.UserUpsertRequest request) {
        String appId = TenantContext.require();
        validate(request, appId, null, true);
        var user = new AppUser(appId, request.username(), request.displayName(), passwordEncoder.encode(request.password()),
                requireRoles(request.roles()), request.allowedMenus(), request.canViewSalary(),
                request.dashboardCustomizationEnabled());
        user.requirePasswordChangeOnNextLogin();
        validateCategory(request.categoryId());
        user.assignCategory(request.categoryId());
        appUserRepository.save(user);
        auditService.record("USER_CREATE", "USER", user.getId(), request.username(), "Created user " + user.getDisplayName(), null);
        return toResponse(user);
    }

    @Transactional
    public AuthApi.UserResponse update(String id, AuthApi.UserUpsertRequest request, String currentUsername) {
        String appId = TenantContext.require();
        var user = appUserRepository.findById(id).filter(item -> item.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("User not found."));
        if (request.version() == null || request.version() != user.getVersion()) {
            throw new BusinessRuleException("This user changed since it was loaded. Refresh and try again.");
        }

        var actorOpt = appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, currentUsername);
        boolean actorIsSuperAdmin = actorOpt.map(u -> u.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.SUPER_ADMIN)).orElse(true);
        boolean targetIsSuperAdmin = user.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.SUPER_ADMIN);

        if (!actorIsSuperAdmin) {
            if (targetIsSuperAdmin) {
                throw new BusinessRuleException("Only a Super Admin can modify or deactivate Super Admin accounts.");
            }
            if (request.roles().contains(RoleCode.SUPER_ADMIN)) {
                throw new BusinessRuleException("Only a Super Admin can assign the Super Admin role.");
            }
        }

        boolean currentlyAdmin = isAdminUser(user);
        boolean willBeAdmin = isAdminRoles(request.roles());
        boolean disablingFinalAdmin = user.isActive() && request.active() == false && currentlyAdmin
                && !willBeAdmin && activeAdminCount(appId) <= 1;
        if (disablingFinalAdmin) {
            throw new BusinessRuleException("لا يمكن تعطيل آخر مسؤول نشط في النظام.", "FINAL_ADMIN_PROTECTION", HttpStatus.BAD_REQUEST);
        }

        validate(request, appId, id, false);
        if (user.getUsername().equalsIgnoreCase(currentUsername) && !request.active()) {
            throw new BusinessRuleException("You cannot deactivate your own account.");
        }
        String passwordHash = request.password() == null || request.password().isBlank()
                ? null : passwordEncoder.encode(request.password());
        user.update(request.username(), request.displayName(), passwordHash, request.active(),
                requireRoles(request.roles()), request.allowedMenus(), request.canViewSalary(),
                request.dashboardCustomizationEnabled());
        if (passwordHash != null) {
            user.markPasswordChanged(Instant.now());
            refreshTokenService.revokeAllForUser(appId, user.getId(), currentUsername);
        }
        validateCategory(request.categoryId());
        user.assignCategory(request.categoryId());
        auditService.record("USER_UPDATE", "USER", user.getId(), currentUsername, "Updated user " + user.getUsername() + " active=" + user.isActive(), null);
        return toResponse(user);
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
                    passwordEncoder.encode(password), roles, Set.of("dashboard","categories","employees","imports","parties","reports","operations","payroll","users","settings","workforce-dashboard","workforce-contractors","workforce-workers","workforce-categories","workforce-requests","workforce-attendance","workforce-settlements","workforce-advances","workforce-accounts","workforce-reports"), true, true));
            created.requirePasswordChangeOnNextLogin();
            return created;
        });
    }

    private void recordFailedLogin(String appId, String username, Instant now) {
        appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username).ifPresent(user -> {
            user.recordFailedLogin(now, MAX_LOGIN_ATTEMPTS, LOCKOUT_DURATION);
            appUserRepository.save(user);
        });
    }

    private Instant issueAccessToken(TenantApplication app, AppUser user, Instant now) {
        return issueAccessToken(app.getId(), user, now);
    }

    private Instant issueAccessToken(String appId, AppUser user, Instant now) {
        var app = tenantApplicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Application not found."));
        Duration ttl = jwtProperties.ttl();
        if (app.isSessionTimeoutEnabled() && app.getSessionTimeoutMinutes() > 0) {
            ttl = Duration.ofMinutes(app.getSessionTimeoutMinutes());
        }
        return now.plus(ttl);
    }

    private String accessToken(String appId, AppUser user, Instant now, Instant expiresAt) {
        var app = tenantApplicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Application not found."));
        var claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("appId", app.getId())
                .claim("appCode", app.getCode())
                .claim("name", user.getDisplayName())
                .claim("tv", user.getTokenVersion())
                .claim("roles", user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private boolean isAdminUser(AppUser user) {
        return user.getRoles().stream().anyMatch(role -> role.getCode() == RoleCode.ADMIN
                || role.getCode() == RoleCode.SUPER_ADMIN);
    }

    private boolean isAdminRoles(Set<RoleCode> roles) {
        return roles.contains(RoleCode.ADMIN) || roles.contains(RoleCode.SUPER_ADMIN);
    }

    private long activeAdminCount(String appId) {
        return appUserRepository.findAllByAppIdOrderByDisplayNameAsc(appId).stream()
                .filter(AppUser::isActive)
                .filter(this::isAdminUser)
                .count();
    }

    private String cookieAppId(String cookieValue) {
        int separator = cookieValue.indexOf(':');
        if (separator <= 0) {
            throw new BusinessRuleException("Session is invalid or expired.", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        return cookieValue.substring(0, separator);
    }

    private String cookieRawToken(String cookieValue) {
        int separator = cookieValue.indexOf(':');
        if (separator <= 0 || separator == cookieValue.length() - 1) {
            throw new BusinessRuleException("Session is invalid or expired.", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        return cookieValue.substring(separator + 1);
    }

    private void validate(AuthApi.UserUpsertRequest request, String appId, String currentId, boolean passwordRequired) {
        if (request.roles() == null || request.roles().isEmpty()) throw new BusinessRuleException("Select at least one role.");
        var app = requireCurrentApp();
        if (passwordRequired && (request.password() == null || request.password().isBlank())) {
            throw new BusinessRuleException("Password is required for a new user.");
        }
        if (request.password() != null && !request.password().isBlank()) {
            validatePasswordStrength(request.password(), app);
        }
        boolean duplicate = currentId == null
                ? appUserRepository.existsByAppIdAndUsernameIgnoreCase(appId, request.username())
                : appUserRepository.existsByAppIdAndUsernameIgnoreCaseAndIdNot(appId, request.username(), currentId);
        if (duplicate) throw new BusinessRuleException("Username already exists.");
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
            throw new BusinessRuleException("كلمة المرور يجب أن لا تحتوي على مسافات.");
        }
        if (app.isRequireUppercase() && !password.chars().anyMatch(Character::isUpperCase)) {
            throw new BusinessRuleException("كلمة المرور يجب أن تحتوي على حرف كبير على الأقل.");
        }
        if (app.isRequireLowercase() && !password.chars().anyMatch(Character::isLowerCase)) {
            throw new BusinessRuleException("كلمة المرور يجب أن تحتوي على حرف صغير على الأقل.");
        }
        if (app.isRequireNumbers() && !password.chars().anyMatch(Character::isDigit)) {
            throw new BusinessRuleException("كلمة المرور يجب أن تحتوي على رقم واحد على الأقل.");
        }
        if (app.isRequireSpecialChars() && !password.matches(".*[^A-Za-z0-9].*")) {
            throw new BusinessRuleException("كلمة المرور يجب أن تحتوي على رمز خاص واحد على الأقل.");
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
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private AuthApi.UserResponse toResponse(AppUser user) {
        return new AuthApi.UserResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRoles().stream().map(Role::getCode).collect(Collectors.toUnmodifiableSet()),
                user.getAllowedMenus(), user.isCanViewSalary(), user.getCategoryId(),
                user.isDashboardCustomizationEnabled(), user.isActive(), user.getVersion());
    }

    private boolean hasRole(AppUser user, RoleCode roleCode) {
        return user.getRoles().stream().anyMatch(role -> role.getCode() == roleCode);
    }

    private void validateCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) return;
        boolean exists = attendanceCategoryRepository.findById(categoryId).filter(c -> c.isActive()).isPresent()
                || workerCategoryRepository.findById(categoryId).filter(c -> "ACTIVE".equals(c.getStatus())).isPresent();
        if (!exists) throw new BusinessRuleException("Select an active category.");
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
                .orElseThrow(() -> new NotFoundException("Application not found."));
    }

    private AuthApi.AppSettingsResponse toSettingsResponse(TenantApplication app) {
        return new AuthApi.AppSettingsResponse(
                app.getSessionTimeoutMinutes(),
                app.isSessionTimeoutEnabled(),
                app.isShowReportPresets(),
                app.getAttendanceAnomalyThresholdPercent(),
                app.isAutomaticProcurementNumbering(),
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

    public record LoginResult(AuthApi.LoginResponse response, String refreshToken, Instant refreshExpiresAt) { }

    public record RefreshResult(AuthApi.RefreshResponse response, String refreshToken, Instant refreshExpiresAt) { }
}
