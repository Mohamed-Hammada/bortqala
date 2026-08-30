package com.bemo.hr.security.pack;

import com.bemo.hr.security.pack.application.PasswordPolicyService;
import com.bemo.hr.security.pack.domain.TenantSecuritySettings;
import com.bemo.hr.security.pack.domain.UserPasswordHistory;
import com.bemo.hr.security.pack.infrastructure.TenantSecuritySettingsRepository;
import com.bemo.hr.security.pack.infrastructure.UserPasswordHistoryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTests {

    @Mock
    private TenantSecuritySettingsRepository settingsRepository;

    @Mock
    private UserPasswordHistoryRepository historyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordPolicyService passwordPolicyService;

    private final String appId = "test-app";
    private final String userId = "user-123";
    private TenantSecuritySettings settings;

    @BeforeEach
    void setUp() {
        passwordPolicyService = new PasswordPolicyService(settingsRepository, historyRepository, passwordEncoder);
        settings = new TenantSecuritySettings(appId);
        settings.setMinPasswordLength(8);
        settings.setRequireUppercase(true);
        settings.setRequireLowercase(true);
        settings.setRequireDigits(true);
        settings.setRequireSpecialChars(true);
        settings.setPasswordHistoryCount(3);
    }

    @Test
    @DisplayName("Valid password passes policy")
    void testValidPasswordPasses() {
        when(settingsRepository.findByAppId(appId)).thenReturn(Optional.of(settings));
        when(historyRepository.findByAppIdAndUserIdOrderByCreatedAtDesc(appId, userId)).thenReturn(List.of());

        assertDoesNotThrow(() -> passwordPolicyService.validatePassword(appId, userId, "Secure123!@#"));
    }

    @Test
    @DisplayName("Short password fails policy")
    void testShortPasswordFails() {
        when(settingsRepository.findByAppId(appId)).thenReturn(Optional.of(settings));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> passwordPolicyService.validatePassword(appId, userId, "Aa1!"));
        assertEquals("PASSWORD_POLICY_TOO_SHORT", ex.getCode());
    }

    @Test
    @DisplayName("Missing uppercase fails policy")
    void testMissingUppercaseFails() {
        when(settingsRepository.findByAppId(appId)).thenReturn(Optional.of(settings));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> passwordPolicyService.validatePassword(appId, userId, "lowercase123!"));
        assertEquals("PASSWORD_POLICY_REQUIRE_UPPERCASE", ex.getCode());
    }

    @Test
    @DisplayName("Password previously used in history fails policy")
    void testPreviouslyUsedPasswordFails() {
        when(settingsRepository.findByAppId(appId)).thenReturn(Optional.of(settings));
        UserPasswordHistory h1 = new UserPasswordHistory(appId, userId, "old-hash-1");
        when(historyRepository.findByAppIdAndUserIdOrderByCreatedAtDesc(appId, userId)).thenReturn(List.of(h1));
        when(passwordEncoder.matches("OldPassword123!", "old-hash-1")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> passwordPolicyService.validatePassword(appId, userId, "OldPassword123!"));
        assertEquals("PASSWORD_POLICY_PREVIOUSLY_USED", ex.getCode());
    }
}
