package com.bemo.hr.shared.security;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.workforce.WorkerCategoryRepository;
import org.springframework.security.authentication.AuthenticationManager;
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
    private final com.bemo.hr.audit.application.AuditService auditService;
    private final TranslationService translationService;
    private final WorkerCategoryRepository workerCategoryRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, JwtProperties jwtProperties,
                       AppUserRepository appUserRepository, RoleRepository roleRepository,
                       TenantApplicationRepository tenantApplicationRepository,
                       UserPreferenceService userPreferenceService, PasswordEncoder passwordEncoder,
                       com.bemo.hr.audit.application.AuditService auditService,
                       TranslationService translationService,
                       WorkerCategoryRepository workerCategoryRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.userPreferenceService = userPreferenceService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.translationService = translationService;
        this.workerCategoryRepository = workerCategoryRepository;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthApi.LoginResponse login(AuthApi.LoginRequest request) {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue(request.appCode())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid credentials."));
        TenantContext.set(app.getId());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    app.getId() + "|" + request.username(), request.password()));
            var user = requireByUsername(app.getId(), request.username());
            Instant now = Instant.now();
            Instant expiresAt = now.plus(Duration.ofMinutes(app.getSessionTimeoutMinutes()));
            var claims = JwtClaimsSet.builder()
                    .issuer(jwtProperties.issuer())
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .subject(user.getUsername())
                    .claim("userId", user.getId())
                    .claim("appId", app.getId())
                    .claim("appCode", app.getCode())
                    .claim("name", user.getDisplayName())
                    .claim("roles", user.getRoles().stream().map(role -> role.getCode().name()).sorted().toList())
                    .build();
            String token = jwtEncoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            auditService.record("USER_LOGIN", "USER", user.getId(), user.getUsername(), "Successful login", null);
            return new AuthApi.LoginResponse(token, "Bearer", expiresAt,
                    new AuthApi.AppResponse(app.getId(), app.getCode(), app.getName()), toResponse(user),
                    toResponse(preferenceFor(user)));
        } finally {
            TenantContext.clear();
        }
    }

    public AuthApi.UserResponse current(String username) {
        return toResponse(requireByUsername(TenantContext.require(), username));
    }

    @Transactional
    public AuthApi.PreferenceResponse currentPreferences(String username) {
        return toResponse(preferenceFor(requireByUsername(TenantContext.require(), username)));
    }

    @Transactional
    public AuthApi.PreferenceResponse updatePreferences(String username, AuthApi.PreferenceRequest request) {
        var user = requireByUsername(TenantContext.require(), username);
        return toResponse(userPreferenceService.update(user.getId(), request));
    }

    public java.util.List<AuthApi.UserResponse> listUsers() {
        return appUserRepository.findAllByAppIdOrderByDisplayNameAsc(TenantContext.require()).stream()
                .map(this::toResponse).toList();
    }

    public List<AuthApi.UserCategoryResponse> listCategories() {
        return workerCategoryRepository.findByStatus("ACTIVE").stream()
                .map(c -> new AuthApi.UserCategoryResponse(c.getId(), c.getCode(), c.getName()))
                .toList();
    }

    public AuthApi.AppSettingsResponse currentAppSettings() {
        return toSettingsResponse(requireCurrentApp());
    }

    @Transactional
    public AuthApi.AppSettingsResponse updateAppSettings(AuthApi.AppSettingsRequest request) {
        var app = requireCurrentApp();
        int minPass = request.minPasswordLength() == null || request.minPasswordLength() <= 0 ? 8 : request.minPasswordLength();
        app.updateSettings(request.sessionTimeoutMinutes(), request.sessionTimeoutEnabled(), request.showReportPresets(), minPass);
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
        auditService.record("SETTINGS_UPDATE", "TENANT_APPLICATION", app.getId(), "ADMIN", "Updated tenant settings and security policy", null);
        return toSettingsResponse(app);
    }

    @Transactional
    public AuthApi.UserResponse create(AuthApi.UserUpsertRequest request) {
        String appId = TenantContext.require();
        validate(request, appId, null, true);
        var user = new AppUser(appId, request.username(), request.displayName(), passwordEncoder.encode(request.password()),
                requireRoles(request.roles()), request.allowedMenus(), request.canViewSalary());
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

        validate(request, appId, id, false);
        if (user.getUsername().equalsIgnoreCase(currentUsername) && !request.active()) {
            throw new BusinessRuleException("You cannot deactivate your own account.");
        }
        String passwordHash = request.password() == null || request.password().isBlank()
                ? null : passwordEncoder.encode(request.password());
        user.update(request.username(), request.displayName(), passwordHash, request.active(), requireRoles(request.roles()), request.allowedMenus(), request.canViewSalary());
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
            return appUserRepository.save(new AppUser(app.getId(), username, displayName,
                    passwordEncoder.encode(password), roles, Set.of("dashboard","categories","employees","imports","parties","reports","operations","payroll","users","settings","workforce-dashboard","workforce-contractors","workforce-workers","workforce-categories","workforce-requests","workforce-attendance","workforce-settlements","workforce-advances","workforce-accounts","workforce-reports"), true));
        });
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
                .or(() -> appUserRepository.findByUsernameIgnoreCase(username).stream().findFirst())
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private AuthApi.UserResponse toResponse(AppUser user) {
        return new AuthApi.UserResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRoles().stream().map(Role::getCode).collect(Collectors.toUnmodifiableSet()),
                user.getAllowedMenus(), user.isCanViewSalary(), user.isActive(), user.getVersion());
    }

    private UserPreference preferenceFor(AppUser user) {
        return userPreferenceService.currentOrCreate(user.getId());
    }

    private AuthApi.PreferenceResponse toResponse(UserPreference preference) {
        return new AuthApi.PreferenceResponse(preference.getTheme(), preference.getTableDensity(),
                preference.getLocale(), preference.getExcelTableStyle(), preference.getDefaultPageSize(),
                preference.getDefaultPage(), preference.getUpdatedAt());
    }

    private TenantApplication requireCurrentApp() {
        String appId = TenantContext.require();
        return tenantApplicationRepository.findById(appId)
                .or(() -> tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("DEMO"))
                .or(() -> tenantApplicationRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new NotFoundException("Application not found."));
    }

    private AuthApi.AppSettingsResponse toSettingsResponse(TenantApplication app) {
        return new AuthApi.AppSettingsResponse(
                app.getSessionTimeoutMinutes(),
                app.isSessionTimeoutEnabled(),
                app.isShowReportPresets(),
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
}
