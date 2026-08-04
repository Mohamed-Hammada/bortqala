package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.domain.BiometricDevice;
import com.bemo.hr.attendance.domain.BiometricSource;
import com.bemo.hr.attendance.domain.ImportBatch;
import com.bemo.hr.attendance.domain.PunchImportEvidence;
import com.bemo.hr.attendance.domain.PunchRecord;
import com.bemo.hr.attendance.infrastructure.BiometricDeviceRepository;
import com.bemo.hr.attendance.infrastructure.BiometricSourceRepository;
import com.bemo.hr.attendance.infrastructure.DeviceCredentialsCrypto;
import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchImportEvidenceRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BiometricDeviceSyncService {
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BiometricSourceRepository biometricSourceRepository;
    private final BiometricDeviceClient biometricDeviceClient;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final PunchImportEvidenceRepository punchImportEvidenceRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;
    private final DeviceCredentialsCrypto deviceCredentialsCrypto;

    public List<ImportApi.DeviceResponse> listDevices() {
        return biometricDeviceRepository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    public List<ImportApi.SourceResponse> listSources() {
        return biometricSourceRepository.findAllByOrderBySourceTypeAscNameAsc().stream()
                .map(source -> new ImportApi.SourceResponse(source.getId(), source.getName(),
                        source.getSourceType().name(), source.getNormalizedCode(), source.isActive(),
                        source.getCreatedAt()))
                .toList();
    }

    @Transactional
    public ImportApi.SourceResponse createSource(ImportApi.SourceRequest request, String actor) {
        BiometricSource.SourceType sourceType = parseSourceType(request.sourceType());
        String normalizedCode = normalizeCode(request.name());
        var source = biometricSourceRepository.findBySourceTypeAndNormalizedCode(sourceType, normalizedCode)
                .orElseGet(() -> biometricSourceRepository.save(new BiometricSource(sourceType, request.name(), normalizedCode)));
        auditService.record("CREATE", "BIOMETRIC_SOURCE", source.getId(), actor,
                "{\"name\":\"" + safe(source.getName()) + "\",\"type\":\"" + sourceType + "\"}", null);
        return toSourceResponse(source);
    }

    @Transactional
    public ImportApi.SourceResponse updateSource(String id, ImportApi.SourceRequest request, String actor) {
        var source = biometricSourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("مصدر البصمة غير موجود.", "BIO_SOURCE_NOT_FOUND"));
        BiometricSource.SourceType sourceType = parseSourceType(request.sourceType());
        String normalizedCode = normalizeCode(request.name());
        var clash = biometricSourceRepository.findBySourceTypeAndNormalizedCode(sourceType, normalizedCode);
        if (clash.isPresent() && !clash.get().getId().equals(source.getId())) {
            throw new BusinessRuleException("A source with this name already exists.", "BIO_SOURCE_NAME_CLASH", HttpStatus.CONFLICT);
        }
        source.update(request.name(), sourceType, normalizedCode, request.active());
        biometricSourceRepository.saveAndFlush(source);
        auditService.record("UPDATE", "BIOMETRIC_SOURCE", id, actor,
                "{\"name\":\"" + safe(source.getName()) + "\",\"active\":" + source.isActive() + "}", null);
        return toSourceResponse(source);
    }

    @Transactional
    public void deleteSource(String id, String actor) {
        var source = biometricSourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("مصدر البصمة غير موجود.", "BIO_SOURCE_NOT_FOUND"));
        biometricSourceRepository.delete(source);
        auditService.record("DELETE", "BIOMETRIC_SOURCE", id, actor, "{}", null);
    }

    @Transactional
    public ImportApi.DeviceResponse create(ImportApi.DeviceRequest request, String actor) {
        validateEndpoint(request.endpointUrl());
        BiometricDevice device = biometricDeviceRepository.save(new BiometricDevice(
                request.name(), request.endpointUrl(), request.enabled(), request.syncIntervalMinutes()));
        device.setCredentials(request.username(), deviceCredentialsCrypto.encrypt(request.password()));
        biometricDeviceRepository.saveAndFlush(device);
        ensureSource(device.getId(), device.getName());
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
        ensureSource(device.getId(), device.getName());
        auditService.record("UPDATE", "BIOMETRIC_DEVICE", id, actor,
                "{\"name\":\"" + safe(device.getName()) + "\",\"enabled\":" + device.isEnabled()
                        + ",\"hasPassword\":" + device.hasPassword() + "}", null);
        return response(device);
    }

    @Transactional
    public ImportApi.DeviceSyncResponse sync(String id, String actor) {
        BiometricDevice device = requireDevice(id);
        BiometricSource source = ensureSource(device.getId(), device.getName());
        try {
            BiometricDeviceClient.DeviceResponse remote = biometricDeviceClient.fetch(device,
                    new BiometricDeviceClient.DeviceCredentials(device.getUsername(),
                            deviceCredentialsCrypto.decrypt(device.getPasswordEncrypted())));
            String checksum = sha256(remote.rawContent());
            var existing = importBatchRepository.findBySourceIdAndChecksum(source.getId(), checksum);
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
                    source.getId(), device.getName(), actor, remote.punches().size(), 0, 0));
            String appId = TenantContext.require();
            int rowNumber = 0;
            List<PunchImportEvidence> evidence = new ArrayList<>(remote.punches().size());
            for (var punch : remote.punches()) {
                rowNumber++;
                String employeeId = employeeRepository.findByEmployeeCodeIgnoreCase(punch.deviceUserId())
                        .or(() -> employeeRepository.findByDeviceUserId(punch.deviceUserId()))
                        .map(employee -> employee.getId()).orElse(null);
                String punchId = UUID.randomUUID().toString();
                int inserted = punchRecordRepository.insertIfAbsent(punchId, appId,
                        batch.getId(), source.getId(), device.getId(), employeeId, punch.deviceUserId(),
                        punch.employeeName(), punch.punchedAt(), punch.rawLine(), rowNumber);
                if (inserted == 1) {
                    imported++;
                    if (latest == null || punch.punchedAt().isAfter(latest)) latest = punch.punchedAt();
                } else {
                    duplicates++;
                    punchId = punchRecordRepository.findBySourceIdAndDeviceUserIdAndPunchedAt(
                                    source.getId(), punch.deviceUserId(), punch.punchedAt())
                            .map(PunchRecord::getId)
                            .orElse(null);
                }
                if (punchId != null) {
                    evidence.add(new PunchImportEvidence(punchId, batch.getId(), appId,
                            rowNumber, punch.rawLine()));
                }
            }
            punchImportEvidenceRepository.saveAll(evidence);
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

    private BiometricSource ensureSource(String deviceId, String deviceName) {
        return biometricSourceRepository.findBySourceTypeAndNormalizedCode(
                        BiometricSource.SourceType.DEVICE, deviceId)
                .orElseGet(() -> {
                    biometricSourceRepository.insertIfAbsent(UUID.randomUUID().toString(),
                            TenantContext.require(), BiometricSource.SourceType.DEVICE.name(),
                            deviceName.strip(), deviceId);
                    return biometricSourceRepository.findBySourceTypeAndNormalizedCode(
                                    BiometricSource.SourceType.DEVICE, deviceId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Biometric source could not be created for device " + deviceId));
                });
    }

    private ImportApi.SourceResponse toSourceResponse(BiometricSource source) {
        return new ImportApi.SourceResponse(source.getId(), source.getName(),
                source.getSourceType().name(), source.getNormalizedCode(), source.isActive(),
                source.getCreatedAt());
    }

    private BiometricSource.SourceType parseSourceType(String value) {
        try {
            return BiometricSource.SourceType.valueOf(value == null ? "FILE_DEVICE" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Unknown biometric source type.", "BIO_SOURCE_TYPE_INVALID", HttpStatus.CONFLICT);
        }
    }

    private String normalizeCode(String name) {
        return name == null ? "" : name.strip().toLowerCase().replaceAll("\\s+", "_");
    }

    private BiometricDevice requireDevice(String id) {
        return biometricDeviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("جهاز البصمة غير موجود.", "BIO_DEVICE_NOT_FOUND"));
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
            throw new BusinessRuleException("رابط جهاز البصمة غير صالح ويجب أن يبدأ بـ http أو https.", "BIO_DEVICE_ENDPOINT_INVALID", HttpStatus.CONFLICT);
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
