package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.domain.BiometricDevice;
import com.bemo.hr.attendance.domain.ImportBatch;
import com.bemo.hr.attendance.domain.PunchRecord;
import com.bemo.hr.attendance.infrastructure.BiometricDeviceRepository;
import com.bemo.hr.attendance.infrastructure.DeviceCredentialsCrypto;
import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BiometricDeviceSyncService {
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BiometricDeviceClient biometricDeviceClient;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;
    private final DeviceCredentialsCrypto deviceCredentialsCrypto;

    public List<ImportApi.DeviceResponse> listDevices() {
        return biometricDeviceRepository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    @Transactional
    public ImportApi.DeviceResponse create(ImportApi.DeviceRequest request, String actor) {
        validateEndpoint(request.endpointUrl());
        BiometricDevice device = biometricDeviceRepository.save(new BiometricDevice(
                request.name(), request.endpointUrl(), request.enabled(), request.syncIntervalMinutes()));
        device.setCredentials(request.username(), deviceCredentialsCrypto.encrypt(request.password()));
        biometricDeviceRepository.saveAndFlush(device);
        auditService.record("CREATE", "BIOMETRIC_DEVICE", device.getId(), actor,
                "{\"name\":\"" + safe(device.getName()) + "\",\"enabled\":" + device.isEnabled()
                        + ",\"hasPassword\":" + device.hasPassword() + "}", null);
        return response(device);
    }

    @Transactional
    public ImportApi.DeviceResponse update(String id, ImportApi.DeviceRequest request, String actor) {
        validateEndpoint(request.endpointUrl());
        BiometricDevice device = requireDevice(id);
        device.update(request.name(), request.endpointUrl(), request.enabled(), request.syncIntervalMinutes());
        if (request.password() != null && !request.password().isBlank()) {
            device.setCredentials(request.username(), deviceCredentialsCrypto.encrypt(request.password()));
        } else {
            device.setCredentials(request.username(), device.getPasswordEncrypted());
        }
        biometricDeviceRepository.saveAndFlush(device);
        auditService.record("UPDATE", "BIOMETRIC_DEVICE", id, actor,
                "{\"name\":\"" + safe(device.getName()) + "\",\"enabled\":" + device.isEnabled()
                        + ",\"hasPassword\":" + device.hasPassword() + "}", null);
        return response(device);
    }

    @Transactional
    public ImportApi.DeviceSyncResponse sync(String id, String actor) {
        BiometricDevice device = requireDevice(id);
        try {
            BiometricDeviceClient.DeviceResponse remote = biometricDeviceClient.fetch(device,
                    new BiometricDeviceClient.DeviceCredentials(device.getUsername(),
                            deviceCredentialsCrypto.decrypt(device.getPasswordEncrypted())));
            String checksum = sha256(remote.rawContent());
            var existing = importBatchRepository.findByChecksum(checksum);
            if (existing.isPresent()) {
                device.syncSucceeded(0, device.getLastSuccessfulPunchAt());
                biometricDeviceRepository.saveAndFlush(device);
                return new ImportApi.DeviceSyncResponse(response(device), remote.punches().size(), 0,
                        remote.punches().size(), true);
            }

            int duplicates = 0;
            int imported = 0;
            Instant latest = device.getLastSuccessfulPunchAt();
            ImportBatch batch = importBatchRepository.save(new ImportBatch(checksum,
                    "device-sync-" + device.getId() + "-" + Instant.now().toEpochMilli() + ".json",
                    device.getName(), actor, remote.punches().size(), 0, 0));
            int rowNumber = 0;
            for (var punch : remote.punches()) {
                rowNumber++;
                if (punchRecordRepository.existsByDeviceUserIdAndPunchedAt(
                        punch.deviceUserId(), punch.punchedAt())) {
                    duplicates++;
                    continue;
                }
                String employeeId = employeeRepository.findByEmployeeCodeIgnoreCase(punch.deviceUserId())
                        .or(() -> employeeRepository.findByDeviceUserId(punch.deviceUserId()))
                        .map(employee -> employee.getId()).orElse(null);
                punchRecordRepository.save(new PunchRecord(batch.getId(), employeeId, punch.deviceUserId(),
                        punch.employeeName(), punch.punchedAt(), punch.rawLine(), rowNumber));
                imported++;
                if (latest == null || punch.punchedAt().isAfter(latest)) latest = punch.punchedAt();
            }
            batch.updateCounts(remote.punches().size(), imported, 0);
            importBatchRepository.save(batch);
            device.syncSucceeded(imported, latest);
            biometricDeviceRepository.saveAndFlush(device);
            auditService.record("SYNC", "BIOMETRIC_DEVICE", id, actor,
                    "{\"received\":" + remote.punches().size() + ",\"imported\":" + imported
                            + ",\"duplicates\":" + duplicates + "}", null);
            return new ImportApi.DeviceSyncResponse(response(device), remote.punches().size(),
                    imported, duplicates, false);
        } catch (RuntimeException exception) {
            device.syncFailed(exception.getMessage());
            biometricDeviceRepository.saveAndFlush(device);
            auditService.record("SYNC_FAILED", "BIOMETRIC_DEVICE", id, actor,
                    "{\"message\":\"" + safe(exception.getMessage()) + "\"}", null);
            return new ImportApi.DeviceSyncResponse(response(device), 0, 0, 0, false);
        }
    }

    public List<BiometricDevice> dueDevices(Instant now) {
        return biometricDeviceRepository.findAllByOrderByNameAsc().stream().filter(device -> device.isDue(now)).toList();
    }

    private BiometricDevice requireDevice(String id) {
        return biometricDeviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("جهاز البصمة غير موجود."));
    }

    private void validateEndpoint(String value) {
        try {
            URI endpoint = URI.create(value.strip());
            if (endpoint.getHost() == null
                    || (!"http".equalsIgnoreCase(endpoint.getScheme())
                    && !"https".equalsIgnoreCase(endpoint.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (Exception exception) {
            throw new BusinessRuleException("رابط جهاز البصمة غير صالح ويجب أن يبدأ بـ http أو https.");
        }
    }

    private ImportApi.DeviceResponse response(BiometricDevice device) {
        return new ImportApi.DeviceResponse(device.getId(), device.getName(), device.getEndpointUrl(),
                device.isEnabled(), device.getSyncIntervalMinutes(), device.getLastSyncAt(),
                device.getLastSuccessfulPunchAt(), device.getNextSyncAt(), device.getLastStatus(),
                device.getLastMessage(), device.getUsername(), device.hasPassword(), device.getCreatedAt());
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
