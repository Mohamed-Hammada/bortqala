package com.bemo.hr.security.pack.application;

import com.bemo.hr.security.pack.domain.UserTotp;
import com.bemo.hr.security.pack.domain.UserTotpBackupCode;
import com.bemo.hr.security.pack.infrastructure.UserTotpBackupCodeRepository;
import com.bemo.hr.security.pack.infrastructure.UserTotpRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@Transactional
public class TotpService {
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserTotpRepository userTotpRepository;
    private final UserTotpBackupCodeRepository backupCodeRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final byte[] encryptionKey;

    public TotpService(UserTotpRepository userTotpRepository,
                       UserTotpBackupCodeRepository backupCodeRepository,
                       AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${hr.security.totp-master-key:bemo-erp-totp-master-key-2026-secure!}") String masterKey) {
        this.userTotpRepository = userTotpRepository;
        this.backupCodeRepository = backupCodeRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionKey = deriveKey(masterKey);
    }

    public record EnrollResult(String secret, String otpauthUri, List<String> backupCodes) {
    }

    public record TotpStatusResult(boolean enabled, Instant enabledAt, int remainingBackupCodes) {
    }

    @Transactional(readOnly = true)
    public TotpStatusResult getStatus(String appId, String userId) {
        Optional<UserTotp> userTotpOpt = userTotpRepository.findByAppIdAndUserId(appId, userId);
        boolean enabled = userTotpOpt.map(UserTotp::isEnabled).orElse(false);
        Instant enabledAt = userTotpOpt.map(UserTotp::getEnabledAt).orElse(null);
        int remainingBackupCodes = enabled ? backupCodeRepository.findByAppIdAndUserIdAndUsedFalse(appId, userId).size() : 0;
        return new TotpStatusResult(enabled, enabledAt, remainingBackupCodes);
    }

    public EnrollResult enroll(String appId, String userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found.", "USER_NOT_FOUND"));

        Optional<UserTotp> existingOpt = userTotpRepository.findByAppIdAndUserId(appId, userId);
        if (existingOpt.isPresent() && existingOpt.get().isEnabled()) {
            throw new BusinessRuleException("Two-factor authentication is already enabled.", "TOTP_ALREADY_ENABLED");
        }

        String rawSecret = generateBase32Secret(20);
        String encryptedSecret = encryptSecret(rawSecret);

        UserTotp totp = existingOpt.orElseGet(() -> new UserTotp(appId, userId, encryptedSecret));
        totp.setSecretEncrypted(encryptedSecret);
        totp.disable();
        userTotpRepository.save(totp);

        // Generate 10 backup codes
        backupCodeRepository.deleteByAppIdAndUserId(appId, userId);
        List<String> plainBackupCodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String code = generateBackupCode();
            plainBackupCodes.add(code);
            String hash = hashBackupCode(code);
            backupCodeRepository.save(new UserTotpBackupCode(appId, userId, hash));
        }

        String label = "BemoERP:" + user.getUsername();
        String otpauthUri = String.format("otpauth://totp/%s?secret=%s&issuer=BemoERP&algorithm=SHA1&digits=6&period=30",
                label, rawSecret);

        return new EnrollResult(rawSecret, otpauthUri, plainBackupCodes);
    }

    public void activate(String appId, String userId, String code) {
        UserTotp totp = userTotpRepository.findByAppIdAndUserId(appId, userId)
                .orElseThrow(() -> new BusinessRuleException("TOTP enrollment not found. Please enroll first.", "TOTP_NOT_ENROLLED"));

        if (totp.isEnabled()) {
            throw new BusinessRuleException("Two-factor authentication is already enabled.", "TOTP_ALREADY_ENABLED");
        }

        String rawSecret = decryptSecret(totp.getSecretEncrypted());
        long currentStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        if (!verifyTotpCode(rawSecret, code, currentStep, 0L)) {
            throw new BusinessRuleException("Invalid verification code. Please check your authenticator app.", "TOTP_INVALID_CODE");
        }

        totp.setLastUsedStep(currentStep);
        totp.enable();
        userTotpRepository.save(totp);
        log.info("2FA activated for user {} in app {}", userId, appId);
    }

    public void disable(String appId, String userId, String password) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found.", "USER_NOT_FOUND"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("Invalid password.", "AUTH_INVALID_CREDENTIALS");
        }

        UserTotp totp = userTotpRepository.findByAppIdAndUserId(appId, userId)
                .orElseThrow(() -> new BusinessRuleException("TOTP is not enabled.", "TOTP_NOT_ENABLED"));

        totp.disable();
        userTotpRepository.save(totp);
        backupCodeRepository.deleteByAppIdAndUserId(appId, userId);
        log.info("2FA disabled for user {} in app {}", userId, appId);
    }

    public List<String> regenerateBackupCodes(String appId, String userId, String codeOrPassword) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found.", "USER_NOT_FOUND"));

        UserTotp totp = userTotpRepository.findByAppIdAndUserId(appId, userId)
                .orElseThrow(() -> new BusinessRuleException("TOTP is not enabled.", "TOTP_NOT_ENABLED"));

        if (!totp.isEnabled()) {
            throw new BusinessRuleException("TOTP is not enabled.", "TOTP_NOT_ENABLED");
        }

        boolean verified = false;
        if (passwordEncoder.matches(codeOrPassword, user.getPasswordHash())) {
            verified = true;
        } else {
            String rawSecret = decryptSecret(totp.getSecretEncrypted());
            long currentStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
            if (verifyTotpCode(rawSecret, codeOrPassword, currentStep, totp.getLastUsedStep())) {
                verified = true;
                totp.setLastUsedStep(currentStep);
            }
        }

        if (!verified) {
            throw new BusinessRuleException("Invalid code or password to regenerate backup codes.", "AUTH_INVALID_CREDENTIALS");
        }

        backupCodeRepository.deleteByAppIdAndUserId(appId, userId);
        List<String> plainBackupCodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String code = generateBackupCode();
            plainBackupCodes.add(code);
            String hash = hashBackupCode(code);
            backupCodeRepository.save(new UserTotpBackupCode(appId, userId, hash));
        }

        userTotpRepository.save(totp);
        return plainBackupCodes;
    }

    public boolean verifyLoginCode(String appId, String userId, String code) {
        UserTotp totp = userTotpRepository.findByAppIdAndUserId(appId, userId)
                .orElse(null);

        if (totp == null || !totp.isEnabled()) {
            return true; // 2FA not enabled
        }

        String cleanCode = code.trim().replace("-", "").replace(" ", "");

        // 1. Try TOTP 6-digit code
        if (cleanCode.length() == 6 && cleanCode.matches("\\d{6}")) {
            String rawSecret = decryptSecret(totp.getSecretEncrypted());
            long currentStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
            if (verifyTotpCode(rawSecret, cleanCode, currentStep, totp.getLastUsedStep())) {
                totp.setLastUsedStep(currentStep);
                userTotpRepository.save(totp);
                return true;
            }
        }

        // 2. Try Backup Code
        List<UserTotpBackupCode> backupCodes = backupCodeRepository.findByAppIdAndUserIdAndUsedFalse(appId, userId);
        String targetHash = hashBackupCode(cleanCode);
        for (UserTotpBackupCode bc : backupCodes) {
            if (bc.getCodeHash().equals(targetHash)) {
                bc.markUsed();
                backupCodeRepository.save(bc);
                log.info("Backup code used by user {} in app {}", userId, appId);
                return true;
            }
        }

        return false;
    }

    public boolean isTotpEnabled(String appId, String userId) {
        return userTotpRepository.findByAppIdAndUserId(appId, userId)
                .map(UserTotp::isEnabled)
                .orElse(false);
    }

    // --- RFC 6238 TOTP Computation ---

    public boolean verifyTotpCode(String base32Secret, String code, long currentStep, long lastUsedStep) {
        try {
            int inputCode = Integer.parseInt(code);
            byte[] key = decodeBase32(base32Secret);

            // Allow window of [-1, 0, +1] time steps
            for (long step = currentStep - 1; step <= currentStep + 1; step++) {
                if (step <= lastUsedStep) {
                    continue; // Replay prevention
                }
                int expectedCode = generateTotpCodeForStep(key, step);
                if (expectedCode == inputCode) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Error verifying TOTP code: {}", e.getMessage());
        }
        return false;
    }

    public int generateTotpCodeForStep(byte[] key, long step) throws Exception {
        byte[] data = ByteBuffer.allocate(8).putLong(step).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "RAW"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);

        return binary % 1_000_000;
    }

    // --- Base32 & Crypto Helpers ---

    private String generateBase32Secret(int numBytes) {
        byte[] bytes = new byte[numBytes];
        SECURE_RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    private String generateBackupCode() {
        // 8 chars alphanumeric, e.g. "ABCD-1234"
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append("-");
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String hashBackupCode(String code) {
        String normalized = code.trim().replace("-", "").toUpperCase(Locale.ROOT);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String encryptSecret(String plainSecret) {
        try {
            byte[] iv = new byte[12];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] cipherText = cipher.doFinal(plainSecret.getBytes(StandardCharsets.UTF_8));

            ByteBuffer bb = ByteBuffer.allocate(iv.length + cipherText.length);
            bb.put(iv);
            bb.put(cipherText);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt TOTP secret", e);
        }
    }

    private String decryptSecret(String encryptedSecret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedSecret);
            ByteBuffer bb = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[12];
            bb.get(iv);
            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt TOTP secret", e);
        }
    }

    private byte[] deriveKey(String masterKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(masterKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_ALPHABET.charAt((buffer >> bitsLeft) & 0x1f));
            }
        }
        if (bitsLeft > 0) {
            buffer <<= (5 - bitsLeft);
            sb.append(BASE32_ALPHABET.charAt(buffer & 0x1f));
        }
        return sb.toString();
    }

    public static byte[] decodeBase32(String base32) {
        String clean = base32.trim().toUpperCase(Locale.ROOT).replace("=", "");
        byte[] result = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : clean.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                result[index++] = (byte) ((buffer >> bitsLeft) & 0xff);
            }
        }
        return Arrays.copyOf(result, index);
    }
}
