package com.bemo.hr.security.pack.application;

import com.bemo.hr.security.pack.domain.RoleIpAllowlist;
import com.bemo.hr.security.pack.domain.TenantSecuritySettings;
import com.bemo.hr.security.pack.infrastructure.RoleIpAllowlistRepository;
import com.bemo.hr.security.pack.infrastructure.TenantSecuritySettingsRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.RoleCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional
public class IpAllowlistService {
    private final RoleIpAllowlistRepository ipAllowlistRepository;
    private final TenantSecuritySettingsRepository settingsRepository;

    public IpAllowlistService(RoleIpAllowlistRepository ipAllowlistRepository,
                              TenantSecuritySettingsRepository settingsRepository) {
        this.ipAllowlistRepository = ipAllowlistRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleIpAllowlist> listRules(String appId) {
        return ipAllowlistRepository.findByAppId(appId);
    }

    public RoleIpAllowlist addRule(String appId, String roleCode, String cidrBlock, String description) {
        validateCidr(cidrBlock);
        RoleIpAllowlist rule = new RoleIpAllowlist(appId, roleCode.trim().toUpperCase(), cidrBlock.trim(), description);
        return ipAllowlistRepository.save(rule);
    }

    public void deleteRule(String appId, String ruleId) {
        RoleIpAllowlist rule = ipAllowlistRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessRuleException("IP rule not found.", "IP_RULE_NOT_FOUND"));
        if (!rule.getAppId().equals(appId)) {
            throw new BusinessRuleException("IP rule not found.", "IP_RULE_NOT_FOUND");
        }
        ipAllowlistRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public void validateClientIp(String appId, Set<RoleCode> roles, String clientIp) {
        if (clientIp == null || clientIp.isBlank() || roles == null || roles.isEmpty()) {
            return;
        }

        // Clean local loopback
        String normalizedIp = clientIp.trim();
        if ("0:0:0:0:0:0:0:1".equals(normalizedIp) || "::1".equals(normalizedIp)) {
            normalizedIp = "127.0.0.1";
        }

        // Check Super Admin bypass
        if (roles.contains(RoleCode.SUPER_ADMIN)) {
            boolean bypass = settingsRepository.findByAppId(appId)
                    .map(TenantSecuritySettings::isSuperAdminIpBypass)
                    .orElse(true);
            if (bypass) {
                return;
            }
        }

        for (RoleCode role : roles) {
            List<RoleIpAllowlist> rules = ipAllowlistRepository.findByAppIdAndRoleCode(appId, role.name());
            if (!rules.isEmpty()) {
                boolean match = false;
                for (RoleIpAllowlist rule : rules) {
                    if (isIpInCidr(normalizedIp, rule.getCidrBlock())) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    log.warn("Login blocked for user with role {} from IP {} (appId: {})", role, normalizedIp, appId);
                    throw new BusinessRuleException(
                            "Access from IP " + normalizedIp + " is not permitted for role " + role.name() + ".",
                            "IP_NOT_ALLOWED"
                    );
                }
            }
        }
    }

    public static void validateCidr(String cidr) {
        if (cidr == null || cidr.isBlank()) {
            throw new BusinessRuleException("CIDR block cannot be empty.", "IP_CIDR_INVALID");
        }
        try {
            String[] parts = cidr.trim().split("/");
            InetAddress.getByName(parts[0]);
            if (parts.length > 1) {
                int prefix = Integer.parseInt(parts[1]);
                if (prefix < 0 || prefix > 32) {
                    throw new BusinessRuleException("CIDR prefix must be between 0 and 32.", "IP_CIDR_INVALID");
                }
            }
        } catch (Exception e) {
            throw new BusinessRuleException("Invalid CIDR format: " + cidr, "IP_CIDR_INVALID");
        }
    }

    public static boolean isIpInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.trim().split("/");
            long ipVal = ipToLong(InetAddress.getByName(ip.trim()));
            long networkVal = ipToLong(InetAddress.getByName(parts[0]));
            int prefix = (parts.length > 1) ? Integer.parseInt(parts[1]) : 32;

            long mask = prefix == 0 ? 0 : (-1L << (32 - prefix)) & 0xFFFFFFFFL;
            return (ipVal & mask) == (networkVal & mask);
        } catch (Exception e) {
            log.warn("Failed checking IP {} against CIDR {}: {}", ip, cidr, e.getMessage());
            return false;
        }
    }

    private static long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result = (result << 8) | (octet & 0xFF);
        }
        return result;
    }
}
