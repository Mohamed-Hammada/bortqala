package com.bemo.hr.platform.deployment.api;

import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.BackupSnapshotResponse;
import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.DrRecoveryStatusResponse;
import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.TriggerBackupRequest;
import com.bemo.hr.platform.deployment.application.DisasterRecoveryBackupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/backups")
public class DisasterRecoveryBackupController {

    private final DisasterRecoveryBackupService backupService;

    public DisasterRecoveryBackupController(DisasterRecoveryBackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('P_SETTINGS_READ') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<List<BackupSnapshotResponse>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasAuthority('P_SETTINGS_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<BackupSnapshotResponse> triggerBackup(
            @RequestBody TriggerBackupRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "system";
        BackupSnapshotResponse response = backupService.triggerBackup(request, username, System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/verify-drill")
    @PreAuthorize("hasAuthority('P_SETTINGS_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<BackupSnapshotResponse> verifyDrill(
            @PathVariable("id") String snapshotId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "system";
        BackupSnapshotResponse response = backupService.executeRestoreDrill(snapshotId, username, System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dr-status")
    @PreAuthorize("hasAuthority('P_SETTINGS_READ') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<DrRecoveryStatusResponse> getDrStatus() {
        return ResponseEntity.ok(backupService.getDrStatus());
    }
}
