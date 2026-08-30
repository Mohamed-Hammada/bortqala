package com.bemo.hr.security.pack.api;

import com.bemo.hr.security.pack.application.IpAllowlistService;
import com.bemo.hr.security.pack.application.PasswordPolicyService;
import com.bemo.hr.security.pack.application.TrustedDeviceService;
import com.bemo.hr.security.pack.domain.RoleIpAllowlist;
import com.bemo.hr.security.pack.domain.TenantSecuritySettings;
import com.bemo.hr.security.pack.domain.TrustedDevice;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityPackController {
    private final PasswordPolicyService passwordPolicyService;
    private final TrustedDeviceService trustedDeviceService;
    private final IpAllowlistService ipAllowlistService;
    private final AuthService authService;

    public SecurityPackController(PasswordPolicyService passwordPolicyService,
                                  TrustedDeviceService trustedDeviceService,
                                  IpAllowlistService ipAllowlistService,
                                  AuthService authService) {
        this.passwordPolicyService = passwordPolicyService;
        this.trustedDeviceService = trustedDeviceService;
        this.ipAllowlistService = ipAllowlistService;
        this.authService = authService;
    }

    @GetMapping("/policy")
    @PreAuthorize("@auth.hasPermission('security:policy:read') or hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<SecurityPackApi.SecurityPolicyResponse> getPolicy() {
        String appId = TenantContext.require();
        TenantSecuritySettings settings = passwordPolicyService.getOrCreateSettings(appId);
        return ResponseEntity.ok(toPolicyResponse(settings));
    }

    @PutMapping("/policy")
    @PreAuthorize("@auth.hasPermission('security:policy:manage') or hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<SecurityPackApi.SecurityPolicyResponse> updatePolicy(
            @Valid @RequestBody SecurityPackApi.SecurityPolicyUpdateRequest request) {
        String appId = TenantContext.require();
        TenantSecuritySettings updated = passwordPolicyService.updateSettings(
                appId,
                request.minPasswordLength(),
                request.requireUppercase(),
                request.requireLowercase(),
                request.requireDigits(),
                request.requireSpecialChars(),
                request.passwordHistoryCount(),
                request.maxPasswordAgeDays(),
                request.sessionTimeoutMinutes(),
                request.superAdminIpBypass()
        );
        return ResponseEntity.ok(toPolicyResponse(updated));
    }

    @GetMapping("/devices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SecurityPackApi.TrustedDeviceResponse>> getDevices(Authentication authentication) {
        String appId = TenantContext.require();
        String userId = authService.getUserIdByUsername(appId, authentication.getName());
        List<TrustedDevice> devices = trustedDeviceService.listDevices(appId, userId);
        List<SecurityPackApi.TrustedDeviceResponse> response = devices.stream()
                .map(this::toDeviceResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/devices/{id}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revokeDevice(@PathVariable("id") String deviceId,
                                             Authentication authentication) {
        String appId = TenantContext.require();
        String userId = authService.getUserIdByUsername(appId, authentication.getName());
        trustedDeviceService.revokeDevice(appId, userId, deviceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ip-rules")
    @PreAuthorize("@auth.hasPermission('security:ip-rules:read') or hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<SecurityPackApi.RoleIpRuleResponse>> getIpRules() {
        String appId = TenantContext.require();
        List<RoleIpAllowlist> rules = ipAllowlistService.listRules(appId);
        List<SecurityPackApi.RoleIpRuleResponse> response = rules.stream()
                .map(r -> new SecurityPackApi.RoleIpRuleResponse(
                        r.getId(),
                        r.getRoleCode(),
                        r.getCidrBlock(),
                        r.getDescription(),
                        r.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ip-rules")
    @PreAuthorize("@auth.hasPermission('security:ip-rules:manage') or hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<SecurityPackApi.RoleIpRuleResponse> createIpRule(
            @Valid @RequestBody SecurityPackApi.RoleIpRuleCreateRequest request) {
        String appId = TenantContext.require();
        RoleIpAllowlist rule = ipAllowlistService.addRule(appId, request.roleCode(), request.cidrBlock(), request.description());
        return ResponseEntity.ok(new SecurityPackApi.RoleIpRuleResponse(
                rule.getId(),
                rule.getRoleCode(),
                rule.getCidrBlock(),
                rule.getDescription(),
                rule.getCreatedAt()
        ));
    }

    @DeleteMapping("/ip-rules/{id}")
    @PreAuthorize("@auth.hasPermission('security:ip-rules:manage') or hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> deleteIpRule(@PathVariable("id") String ruleId) {
        String appId = TenantContext.require();
        ipAllowlistService.deleteRule(appId, ruleId);
        return ResponseEntity.noContent().build();
    }

    private SecurityPackApi.SecurityPolicyResponse toPolicyResponse(TenantSecuritySettings s) {
        return new SecurityPackApi.SecurityPolicyResponse(
                s.getMinPasswordLength(),
                s.isRequireUppercase(),
                s.isRequireLowercase(),
                s.isRequireDigits(),
                s.isRequireSpecialChars(),
                s.getPasswordHistoryCount(),
                s.getMaxPasswordAgeDays(),
                s.getSessionTimeoutMinutes(),
                s.isSuperAdminIpBypass()
        );
    }

    private SecurityPackApi.TrustedDeviceResponse toDeviceResponse(TrustedDevice d) {
        return new SecurityPackApi.TrustedDeviceResponse(
                d.getId(),
                d.getDeviceId(),
                d.getDeviceLabel(),
                d.getUserAgent(),
                d.getIpAddress(),
                d.getLastSeenAt(),
                d.isRevoked(),
                d.getRevokedAt()
        );
    }
}
