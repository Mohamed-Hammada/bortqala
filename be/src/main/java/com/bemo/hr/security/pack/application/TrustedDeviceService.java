package com.bemo.hr.security.pack.application;

import com.bemo.hr.security.pack.domain.TrustedDevice;
import com.bemo.hr.security.pack.infrastructure.TrustedDeviceRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class TrustedDeviceService {
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final AppUserRepository appUserRepository;

    public TrustedDeviceService(TrustedDeviceRepository trustedDeviceRepository,
                                AppUserRepository appUserRepository) {
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.appUserRepository = appUserRepository;
    }

    public void recordDeviceActivity(String appId, String userId, String rawDeviceId, String userAgent, String ipAddress) {
        if (appId == null || userId == null) {
            return;
        }
        String deviceId = (rawDeviceId != null && !rawDeviceId.isBlank())
                ? rawDeviceId.trim()
                : "browser-" + Integer.toHexString((userAgent + ipAddress).hashCode());

        String deviceLabel = parseDeviceLabel(userAgent);

        Optional<TrustedDevice> existingOpt = trustedDeviceRepository.findByAppIdAndUserIdAndDeviceId(appId, userId, deviceId);
        if (existingOpt.isPresent()) {
            TrustedDevice device = existingOpt.get();
            device.recordActivity(deviceLabel, userAgent, ipAddress);
            trustedDeviceRepository.save(device);
        } else {
            TrustedDevice newDevice = new TrustedDevice(appId, userId, deviceId, deviceLabel, userAgent, ipAddress);
            trustedDeviceRepository.save(newDevice);
        }
    }

    @Transactional(readOnly = true)
    public List<TrustedDevice> listDevices(String appId, String userId) {
        return trustedDeviceRepository.findByAppIdAndUserIdOrderByLastSeenAtDesc(appId, userId);
    }

    public void revokeDevice(String appId, String userId, String deviceRecordId) {
        TrustedDevice device = trustedDeviceRepository.findById(deviceRecordId)
                .orElseThrow(() -> new BusinessRuleException("Device not found.", "DEVICE_NOT_FOUND"));

        if (!device.getAppId().equals(appId) || !device.getUserId().equals(userId)) {
            throw new BusinessRuleException("Device not found.", "DEVICE_NOT_FOUND");
        }

        if (device.isRevoked()) {
            throw new BusinessRuleException("Device has already been revoked.", "DEVICE_ALREADY_REVOKED");
        }

        device.revoke();
        trustedDeviceRepository.save(device);

        // Invalidate active JWT refresh tokens by bumping tokenVersion on user
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found.", "USER_NOT_FOUND"));
        user.bumpTokenVersion();
        appUserRepository.save(user);

        log.info("Revoked trusted device {} for user {} in app {}, tokenVersion bumped to {}",
                device.getDeviceId(), userId, appId, user.getTokenVersion());
    }

    private String parseDeviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Device";
        }
        String os = "Unknown OS";
        if (userAgent.contains("Windows")) os = "Windows";
        else if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS")) os = "macOS";
        else if (userAgent.contains("Android")) os = "Android";
        else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) os = "iOS";
        else if (userAgent.contains("Linux")) os = "Linux";

        String browser = "Browser";
        if (userAgent.contains("Edg/")) browser = "Edge";
        else if (userAgent.contains("Chrome/")) browser = "Chrome";
        else if (userAgent.contains("Firefox/")) browser = "Firefox";
        else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) browser = "Safari";
        else if (userAgent.contains("Capacitor")) browser = "Bemo Mobile App";

        return browser + " on " + os;
    }
}
