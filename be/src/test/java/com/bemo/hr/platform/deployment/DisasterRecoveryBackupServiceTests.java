package com.bemo.hr.platform.deployment;

import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.BackupSnapshotResponse;
import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.DrRecoveryStatusResponse;
import com.bemo.hr.platform.deployment.api.PlatformDeploymentApi.TriggerBackupRequest;
import com.bemo.hr.platform.deployment.application.DisasterRecoveryBackupService;
import com.bemo.hr.platform.deployment.domain.BackupSnapshot;
import com.bemo.hr.platform.deployment.domain.DrDrillRecord;
import com.bemo.hr.platform.deployment.infrastructure.BackupSnapshotRepository;
import com.bemo.hr.platform.deployment.infrastructure.DrDrillRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisasterRecoveryBackupServiceTests {

    @Mock
    private BackupSnapshotRepository backupRepository;

    @Mock
    private DrDrillRecordRepository drillRepository;

    private DisasterRecoveryBackupService backupService;

    @BeforeEach
    void setUp() {
        backupService = new DisasterRecoveryBackupService(backupRepository, drillRepository);
    }

    @Test
    @DisplayName("triggerBackup creates full encrypted snapshot with valid SHA-256 checksum")
    void triggerBackupSuccess() {
        when(backupRepository.save(any(BackupSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        TriggerBackupRequest request = new TriggerBackupRequest("DAILY_PROD_BACKUP", "FULL");
        BackupSnapshotResponse response = backupService.triggerBackup(request, "admin", 1755600000000L);

        assertThat(response).isNotNull();
        assertThat(response.snapshotName()).isEqualTo("DAILY_PROD_BACKUP");
        assertThat(response.backupType()).isEqualTo("FULL");
        assertThat(response.encryptionAlgorithm()).isEqualTo("AES_256_GCM");
        assertThat(response.sha256Checksum()).hasSize(64);
        assertThat(response.verificationDrillStatus()).isEqualTo("PENDING");

        verify(backupRepository).save(any(BackupSnapshot.class));
    }

    @Test
    @DisplayName("executeRestoreDrill verifies snapshot and creates DR drill record")
    void executeRestoreDrillSuccess() {
        BackupSnapshot snapshot = new BackupSnapshot(
                "TEST_SNAPSHOT",
                "FULL",
                1000000L,
                "a1b2c3d4e5f6",
                "AES_256_GCM",
                "s3://backups/test.enc",
                "COMPLETED",
                "PENDING",
                1755600000000L,
                1765600000000L
        );
        when(backupRepository.findById("snap-1")).thenReturn(Optional.of(snapshot));
        when(backupRepository.save(any(BackupSnapshot.class))).thenAnswer(i -> i.getArgument(0));
        when(drillRepository.save(any(DrDrillRecord.class))).thenAnswer(i -> i.getArgument(0));

        BackupSnapshotResponse response = backupService.executeRestoreDrill("snap-1", "sec_admin", 1755605000000L);

        assertThat(response.verificationDrillStatus()).isEqualTo("PASSED");
        assertThat(response.status()).isEqualTo("VERIFIED");
        assertThat(response.verifiedBy()).isEqualTo("sec_admin");

        verify(drillRepository).save(any(DrDrillRecord.class));
    }

    @Test
    @DisplayName("getDrStatus evaluates RPO and RTO compliance against targets")
    void getDrStatusCompliance() {
        DrDrillRecord drill = new DrDrillRecord(
                "DRILL_1",
                15,
                60,
                8,
                12,
                "SUCCESS",
                "{}",
                "admin",
                1755600000000L
        );
        when(drillRepository.findAllOrdered()).thenReturn(List.of(drill));

        DrRecoveryStatusResponse status = backupService.getDrStatus();

        assertThat(status.status()).isEqualTo("COMPLIANT");
        assertThat(status.actualRpoMinutes()).isEqualTo(8);
        assertThat(status.actualRtoMinutes()).isEqualTo(12);
        assertThat(status.recentDrills()).hasSize(1);
    }
}
