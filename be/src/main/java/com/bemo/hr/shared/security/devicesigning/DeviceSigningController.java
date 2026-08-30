package com.bemo.hr.shared.security.devicesigning;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/devices")
public class DeviceSigningController {

    private final DeviceSigningService deviceSigningService;
    private final AppUserRepository appUserRepository;

    public DeviceSigningController(DeviceSigningService deviceSigningService, AppUserRepository appUserRepository) {
        this.deviceSigningService = deviceSigningService;
        this.appUserRepository = appUserRepository;
    }

    private AppUser getCurrentUser(Authentication authentication) {
        return appUserRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BusinessRuleException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public DeviceSigningApi.DeviceListResponse listDevices(Authentication authentication) {
        AppUser user = getCurrentUser(authentication);
        List<UserDevice> devices = deviceSigningService.listUserDevices(user.getId());
        List<DeviceSigningApi.DeviceResponse> dtos = devices.stream()
                .map(DeviceSigningApi.DeviceResponse::from)
                .toList();
        return new DeviceSigningApi.DeviceListResponse(dtos);
    }

    @PostMapping("/enroll")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceSigningApi.DeviceResponse enrollDevice(@Valid @RequestBody DeviceSigningApi.EnrollDeviceRequest request,
                                                        Authentication authentication) {
        AppUser user = getCurrentUser(authentication);
        UserDevice device = deviceSigningService.enrollDevice(
                user.getId(),
                user.getUsername(),
                request.deviceIdentifier(),
                request.deviceName(),
                request.publicKey(),
                request.algorithm()
        );
        return DeviceSigningApi.DeviceResponse.from(device);
    }

    @PostMapping("/{deviceId}/revoke")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeDevice(@PathVariable String deviceId,
                             @RequestBody(required = false) DeviceSigningApi.RevokeDeviceRequest request,
                             Authentication authentication) {
        AppUser user = getCurrentUser(authentication);
        String reason = request != null ? request.reason() : "REVOKED_BY_USER";
        deviceSigningService.revokeDevice(user.getId(), user.getUsername(), deviceId, reason);
    }

    @PostMapping("/challenge")
    @PreAuthorize("isAuthenticated()")
    public DeviceSigningApi.ChallengeResponse createChallenge(@Valid @RequestBody DeviceSigningApi.CreateChallengeRequest request,
                                                              Authentication authentication) {
        AppUser user = getCurrentUser(authentication);
        DeviceSigningChallenge challenge = deviceSigningService.createChallenge(
                user.getId(),
                request.deviceId(),
                request.operationType(),
                request.payload()
        );
        return new DeviceSigningApi.ChallengeResponse(
                challenge.getId(),
                challenge.getDeviceId(),
                challenge.getNonce(),
                challenge.getOperationType(),
                challenge.getPayloadHash(),
                challenge.getExpiresAt()
        );
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public DeviceSigningApi.VerificationResponse verifySignature(@Valid @RequestBody DeviceSigningApi.VerifySignatureRequest request,
                                                                 Authentication authentication) {
        AppUser user = getCurrentUser(authentication);
        boolean verified = deviceSigningService.verifySignature(
                user.getId(),
                user.getUsername(),
                request.challengeId(),
                request.signature(),
                request.payload()
        );
        return new DeviceSigningApi.VerificationResponse(
                verified,
                request.challengeId(),
                null,
                null,
                Instant.now()
        );
    }
}
