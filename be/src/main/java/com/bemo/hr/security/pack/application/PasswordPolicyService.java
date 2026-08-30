package com.bemo.hr.security.pack.application;

import com.bemo.hr.security.pack.domain.TenantSecuritySettings;
import com.bemo.hr.security.pack.domain.UserPasswordHistory;
import com.bemo.hr.security.pack.infrastructure.TenantSecuritySettingsRepository;
import com.bemo.hr.security.pack.infrastructure.UserPasswordHistoryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
public class PasswordPolicyService {
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    private final TenantSecuritySettingsRepository settingsRepository;
    private final UserPasswordHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyService(TenantSecuritySettingsRepository settingsRepository,
                                 UserPasswordHistoryRepository historyRepository,
                                 PasswordEncoder passwordEncoder) {
        this.settingsRepository = settingsRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TenantSecuritySettings getOrCreateSettings(String appId) {
        return settingsRepository.findByAppId(appId)
                .orElseGet(() -> settingsRepository.save(new TenantSecuritySettings(appId)));
    }

    public void validatePassword(String appId, String userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessRuleException("Password cannot be empty.", "PASSWORD_POLICY_TOO_SHORT");
        }

        TenantSecuritySettings settings = getOrCreateSettings(appId);

        if (newPassword.length() < settings.getMinPasswordLength()) {
            throw new BusinessRuleException(
                    "Password must be at least " + settings.getMinPasswordLength() + " characters long.",
                    "PASSWORD_POLICY_TOO_SHORT"
            );
        }

        if (settings.isRequireUppercase() && !UPPERCASE_PATTERN.matcher(newPassword).find()) {
            throw new BusinessRuleException("Password must contain at least one uppercase letter.", "PASSWORD_POLICY_REQUIRE_UPPERCASE");
        }

        if (settings.isRequireLowercase() && !LOWERCASE_PATTERN.matcher(newPassword).find()) {
            throw new BusinessRuleException("Password must contain at least one lowercase letter.", "PASSWORD_POLICY_REQUIRE_LOWERCASE");
        }

        if (settings.isRequireDigits() && !DIGIT_PATTERN.matcher(newPassword).find()) {
            throw new BusinessRuleException("Password must contain at least one digit.", "PASSWORD_POLICY_REQUIRE_DIGIT");
        }

        if (settings.isRequireSpecialChars() && !SPECIAL_PATTERN.matcher(newPassword).find()) {
            throw new BusinessRuleException("Password must contain at least one special character.", "PASSWORD_POLICY_REQUIRE_SPECIAL");
        }

        // Check password history
        if (userId != null && settings.getPasswordHistoryCount() > 0) {
            List<UserPasswordHistory> history = historyRepository.findByAppIdAndUserIdOrderByCreatedAtDesc(appId, userId);
            int checkCount = Math.min(history.size(), settings.getPasswordHistoryCount());
            for (int i = 0; i < checkCount; i++) {
                UserPasswordHistory entry = history.get(i);
                if (passwordEncoder.matches(newPassword, entry.getPasswordHash())) {
                    throw new BusinessRuleException(
                            "Cannot reuse any of your last " + settings.getPasswordHistoryCount() + " passwords.",
                            "PASSWORD_POLICY_PREVIOUSLY_USED"
                    );
                }
            }
        }
    }

    public void recordPasswordChange(String appId, String userId, String passwordHash) {
        if (userId != null && passwordHash != null) {
            historyRepository.save(new UserPasswordHistory(appId, userId, passwordHash));
        }
    }

    public TenantSecuritySettings updateSettings(String appId,
                                                 int minLength,
                                                 boolean requireUpper,
                                                 boolean requireLower,
                                                 boolean requireDigits,
                                                 boolean requireSpecial,
                                                 int historyCount,
                                                 int maxAgeDays,
                                                 int sessionTimeout,
                                                 boolean superAdminBypass) {
        TenantSecuritySettings settings = getOrCreateSettings(appId);
        settings.setMinPasswordLength(Math.max(6, Math.min(128, minLength)));
        settings.setRequireUppercase(requireUpper);
        settings.setRequireLowercase(requireLower);
        settings.setRequireDigits(requireDigits);
        settings.setRequireSpecialChars(requireSpecial);
        settings.setPasswordHistoryCount(Math.max(0, Math.min(24, historyCount)));
        settings.setMaxPasswordAgeDays(Math.max(0, Math.min(365, maxAgeDays)));
        settings.setSessionTimeoutMinutes(Math.max(5, Math.min(1440, sessionTimeout)));
        settings.setSuperAdminIpBypass(superAdminBypass);
        return settingsRepository.save(settings);
    }
}
