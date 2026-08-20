package com.bemo.hr.platform.deployment.api;

import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.InstallLicenseRequest;
import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.LicenseStatusResponse;
import com.bemo.hr.platform.deployment.application.OfflineLicensingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/licensing")
public class OfflineLicensingController {

    private final OfflineLicensingService licensingService;

    public OfflineLicensingController(OfflineLicensingService licensingService) {
        this.licensingService = licensingService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('P_SETTINGS_READ') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<LicenseStatusResponse> getLicenseStatus() {
        return ResponseEntity.ok(licensingService.validateCurrentLicense(System.currentTimeMillis()));
    }

    @PostMapping("/install")
    @PreAuthorize("hasAuthority('P_SETTINGS_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<LicenseStatusResponse> installCertificate(@RequestBody InstallLicenseRequest request) {
        LicenseStatusResponse response = licensingService.installCertificate(request, System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('P_SETTINGS_READ') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<LicenseStatusResponse> validateCertificate() {
        return ResponseEntity.ok(licensingService.validateCurrentLicense(System.currentTimeMillis()));
    }
}
