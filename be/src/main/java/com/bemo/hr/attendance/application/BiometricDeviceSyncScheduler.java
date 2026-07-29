package com.bemo.hr.attendance.application;

import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BiometricDeviceSyncScheduler {
    private final TenantApplicationRepository tenantApplicationRepository;
    private final BiometricDeviceSyncService biometricDeviceSyncService;

    @Scheduled(initialDelayString = "${hr.biometric-sync.initial-delay-ms:60000}",
            fixedDelayString = "${hr.biometric-sync.poll-delay-ms:60000}")
    public void synchronizeDueDevices() {
        tenantApplicationRepository.findAll().stream().filter(app -> app.isActive()).forEach(app -> {
            try {
                TenantContext.set(app.getId());
                biometricDeviceSyncService.dueDevices(Instant.now())
                        .forEach(device -> {
                            try {
                                biometricDeviceSyncService.sync(device.getId(), "scheduled-device-sync");
                            } catch (RuntimeException ignored) {
                                // The service persists the failure status for administrators to review.
                            }
                        });
            } finally {
                TenantContext.clear();
            }
        });
    }
}
