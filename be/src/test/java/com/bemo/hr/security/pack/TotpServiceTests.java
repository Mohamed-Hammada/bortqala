package com.bemo.hr.security.pack;

import com.bemo.hr.security.pack.application.TotpService;
import com.bemo.hr.security.pack.domain.UserTotp;
import com.bemo.hr.security.pack.domain.UserTotpBackupCode;
import com.bemo.hr.security.pack.infrastructure.UserTotpBackupCodeRepository;
import com.bemo.hr.security.pack.infrastructure.UserTotpRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotpServiceTests {

    @Mock
    private UserTotpRepository userTotpRepository;

    @Mock
    private UserTotpBackupCodeRepository backupCodeRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TotpService totpService;

    private final String appId = "test-app-id";
    private final String userId = "test-user-id";
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        totpService = new TotpService(
                userTotpRepository,
                backupCodeRepository,
                appUserRepository,
                passwordEncoder,
                "test-master-key-for-unit-tests-secure!"
        );
        testUser = new AppUser(appId, "admin", "Admin User", "encoded-pass", java.util.Set.of(), java.util.Set.of(), true, true);
    }

    @Test
    @DisplayName("Enroll generates secret, otpauth URI, and 10 backup codes")
    void testEnrollGeneratesSecretAndBackupCodes() {
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userTotpRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.empty());

        TotpService.EnrollResult result = totpService.enroll(appId, userId);

        assertNotNull(result);
        assertNotNull(result.secret());
        assertTrue(result.otpauthUri().contains("otpauth://totp/"));
        assertEquals(10, result.backupCodes().size());
        verify(userTotpRepository).save(any(UserTotp.class));
        verify(backupCodeRepository, times(10)).save(any(UserTotpBackupCode.class));
    }

    @Test
    @DisplayName("RFC 6238 code computation and verification")
    void testRfc6238TotpCodeVerification() throws Exception {
        String base32Secret = "JBSWY3DPEHPK3PXP"; // Standard RFC test vector secret
        byte[] key = TotpService.decodeBase32(base32Secret);
        long step = 1000L;

        int expectedCode = totpService.generateTotpCodeForStep(key, step);
        String codeStr = String.format("%06d", expectedCode);

        // Verification matches exact time step
        boolean valid = totpService.verifyTotpCode(base32Secret, codeStr, step, 0L);
        assertTrue(valid);

        // Verification matches step - 1 (within drift window)
        boolean validMinusOne = totpService.verifyTotpCode(base32Secret, codeStr, step + 1, 0L);
        assertTrue(validMinusOne);

        // Verification matches step + 1 (within drift window)
        boolean validPlusOne = totpService.verifyTotpCode(base32Secret, codeStr, step - 1, 0L);
        assertTrue(validPlusOne);

        // Verification fails if replay on same step
        boolean replayFailed = totpService.verifyTotpCode(base32Secret, codeStr, step, step);
        assertFalse(replayFailed);

        // Verification fails with invalid code
        boolean invalidCode = totpService.verifyTotpCode(base32Secret, "999999", step, 0L);
        assertFalse(invalidCode);
    }

    @Test
    @DisplayName("Verify backup code marks code as used")
    void testVerifyBackupCodeMarksUsed() {
        UserTotp totp = new UserTotp(appId, userId, "encrypted-secret");
        totp.enable();
        when(userTotpRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.of(totp));

        // Generate a fake backup code
        String plainCode = "ABCD-1234";
        String normalized = "ABCD1234";
        String hash;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            hash = java.util.HexFormat.of().formatHex(md.digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        UserTotpBackupCode backupCode = new UserTotpBackupCode(appId, userId, hash);
        when(backupCodeRepository.findByAppIdAndUserIdAndUsedFalse(appId, userId)).thenReturn(List.of(backupCode));

        boolean verified = totpService.verifyLoginCode(appId, userId, plainCode);
        assertTrue(verified);
        assertTrue(backupCode.isUsed());
        verify(backupCodeRepository).save(backupCode);
    }
}
