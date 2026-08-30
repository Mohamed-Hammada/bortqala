package com.bemo.hr.product.pack;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryRoleProvisioningService {
    private final AppUserRepository userRepository;

    public void validateRoleCode(String roleCode) {
        try {
            RoleCode.valueOf(roleCode);
        } catch (Exception ex) {
            log.warn("Unknown role code in industry pack: {}", roleCode);
            throw new BusinessRuleException("INDUSTRY_PACK_ROLE_UNKNOWN", "INDUSTRY_PACK_ROLE_UNKNOWN", HttpStatus.BAD_REQUEST);
        }
    }

    public void validateRoles(List<String> roleCodes) {
        if (roleCodes != null) {
            for (String code : roleCodes) {
                validateRoleCode(code);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<RoleStatus> evaluateRoleStatus(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        String appId = TenantContext.require();
        List<AppUser> activeUsers = userRepository.findAllByAppIdOrderByDisplayNameAsc(appId).stream()
                .filter(AppUser::isActive)
                .toList();

        List<RoleStatus> results = new ArrayList<>();
        for (String roleStr : roleCodes) {
            validateRoleCode(roleStr);
            RoleCode roleCode = RoleCode.valueOf(roleStr);
            long assignedUsers = activeUsers.stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getCode() == roleCode))
                    .count();
            String status = assignedUsers > 0 ? "ASSIGNED" : "AVAILABLE_UNASSIGNED";
            results.add(new RoleStatus(roleStr, true, true, assignedUsers, status));
        }
        return results;
    }

    public record RoleStatus(
            String code,
            boolean required,
            boolean available,
            long assignedUsers,
            String status
    ) {}
}
