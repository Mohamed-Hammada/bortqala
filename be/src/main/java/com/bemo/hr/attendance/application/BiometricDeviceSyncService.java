package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.domain.*;
import com.bemo.hr.attendance.infrastructure.*;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

@Slf4j
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
    private final BiometricEmployeeProvisioningService employeeProvisioningService;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;
    private final DeviceCredentialsCrypto deviceCredentialsCrypto;

    public List<ImportApi.DeviceResponse> listDevices() {
        log.debug("listDevices called");
        return biometricDeviceRepository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    public List<ImportApi.SourceResponse> listSources() {
        log.debug("listSources called");
        return biometricSourceRepository.findAllByOrderBySourceTypeAscNameAsc().stream()
                .map(this::toSourceResponse)
                .toList();
    }

    @Transactional
    public ImportApi.SourceResponse createSource(ImportApi.SourceRequest request, String actor) {
        log.debug("createSource called with name={}, sourceType={}", request.name(), request.sourceType());
        if (request.name() == null || request.name().isBlank()) {
            log.warn("Validation failed: source name is required");
            throw new BusinessRuleException("Source name is required.", "BIO_SOURCE_NAME_REQUIRED", HttpStatus.CONFLICT);
        }
        BiometricSource.SourceType sourceType = parseSourceType(request.sourceType());
        if (sourceType != BiometricSource.SourceType.FILE_DEVICE) {
            throw new BusinessRuleException("Device sources are registered automatically from live devices and cannot be created manually.",
                    "BIO_SOURCE_DEVICE_TYPE_RESTRICTED", HttpStatus.CONFLICT);
        }
        String normalizedCode = normalizeCode(request.name());
        boolean active = request.active() == null || request.active();
        var source = biometricSourceRepository.findBySourceTypeAndNormalizedCode(sourceType, normalizedCode)
                .orElseGet(() -> new BiometricSource(sourceType, request.name(), normalizedCode, active));
        boolean autoCreate = Boolean.TRUE.equals(request.autoCreateEmployees());
        boolean autoCreateActive = request.autoCreateEmployeeActive() == null || request.autoCreateEmployeeActive();
        employeeProvisioningService.configureSource(source, autoCreate,
                request.autoCreateCategoryId(), request.autoCreateEmploymentType(),
                request.autoCreateActiveFromMode(), autoCreateActive);
        source = biometricSourceRepository.saveAndFlush(source);
        log.info("BiometricSource {} created successfully with id={}", sourceType, source.getId());
        auditService.record("CREATE", "BIOMETRIC_SOURCE", source.getId(), actor,
                "{\"name\":\"" + safe(source.getName()) + "\",\"type\":\"" + sourceType + "\"}", null);
        return toSourceResponse(source);
    }

    @Transactional
    public ImportApi.SourceResponse updateSource(String id, ImportApi.SourceRequest request, String actor) {
        log.debug("updateSource called with id={}", id);
        var source = biometricSourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Biometric source not found.", "BIO_SOURCE_NOT_FOUND"));
        boolean autoCreate = Boolean.TRUE.equals(request.autoCreateEmployees());
        boolean autoCreateActive = request.autoCreateEmployeeActive() == null || request.autoCreateEmployeeActive();
        if (source.getSourceType() == BiometricSource.SourceType.DEVICE) {
            if (request.sourceType() != null
                    && !BiometricSource.SourceType.DEVICE.name().equalsIgnoreCase(request.sourceType())) {
                throw new BusinessRuleException("Device source identity is immutable.",
                        "BIO_SOURCE_DEVICE_IMMUTABLE", HttpStatus.CONFLICT);
            }
            employeeProvisioningService.configureSource(source, autoCreate,
                    request.autoCreateCategoryId(), request.autoCreateEmploymentType(),
                    request.autoCreateActiveFromMode(), autoCreateActive);
            biometricSourceRepository.saveAndFlush(source);
            auditService.record("UPDATE", "BIOMETRIC_SOURCE", id, actor,
                    "{\"name\":\"" + safe(source.getName()) + "\",\"active\":" + source.isActive() + "}", null);
            return toSourceResponse(source);
        }
        BiometricSource.SourceType sourceType = parseSourceType(request.sourceType());
        if (sourceType != BiometricSource.SourceType.FILE_DEVICE) {
            throw new BusinessRuleException("Device sources are registered automatically from live devices and cannot be created manually.",
                    "BIO_SOURCE_DEVICE_TYPE_RESTRICTED", HttpStatus.CONFLICT);
        }
        String normalizedCode = normalizeCode(request.name());
        var clash = biometricSourceRepository.findBySourceTypeAndNormalizedCode(sourceType, normalizedCode);
        if (clash.isPresent() && !clash.get().getId().equals(source.getId())) {
            throw new BusinessRuleException("A source with this name already exists.", "BIO_SOURCE_NAME_CLASH", HttpStatus.CONFLICT);
        }
        boolean active = request.active() == null || request.active();
        source.update(request.name(), sourceType, normalizedCode, active);
        employeeProvisioningService.configureSource(source, autoCreate,
                request.autoCreateCategoryId(), request.autoCreateEmploymentType(),
                request.autoCreateActiveFromMode(), autoCreateActive);
        biometricSourceRepository.saveAndFlush(source);
        auditService.record("UPDATE", "BIOMETRIC_SOURCE", id, actor,
                "{\"name\":\"" + safe(source.getName()) + "\",\"active\":" + source.isActive() + "}", null);
        return toSourceResponse(source);
    }

    @Transactional
    public void deleteSource(String id, String actor) {
        log.debug("deleteSource called with id={}", id);
        var source = biometricSourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Biometric source not found.", "BIO_SOURCE_NOT_FOUND"));
        if (source.getSourceType() == BiometricSource.SourceType.DEVICE) {
            throw new BusinessRuleException("Device sources are immutable and cannot be deleted while the device is registered.",
                    "BIO_SOURCE_DEVICE_IMMUTABLE", HttpStatus.CONFLICT);
        }
        biometricSourceRepository.delete(source);
        log.info("BiometricSource deleted successfully with id={}", id);
        auditService.record("DELETE", "BIOMETRIC_SOURCE", id, actor, "{}", null);
    }

    @Transactional
    public ImportApi.DeviceResponse create(ImportApi.DeviceRequest request, String actor) {
        log.debug("create called with name={}", request.name());
        validateEndpoint(request.endpointUrl());
        BiometricDevice device = biometricDeviceRepository.save(new BiometricDevice(
                request.name(), request.endpointUrl(), request.enabled(), request.syncIntervalMinutes()));
        device.setCredentials(request.username(), deviceCredentialsCrypto.encrypt(request.password()));
        biometricDeviceRepository.saveAndFlush(device);
        ensureSource(device.getId(), device.getName());
        log.info("BiometricDevice created successfully with id={}", device.getId());
        auditService.record("CREATE", "BIOMETRIC_DEVICE", device.getId(), actor,
                "{\"name\":\"" + safe(device.getName()) + "\",\"enabled\":" + device.isEnabled()
                        + ",\"hasPassword\":" + device.hasPassword() + "}", null);
        return response(device);
    }

    @Transactional
    public ImportApi.DeviceResponse update(String id, ImportApi.DeviceRequest request, String actor) {
        log.debug("update called with id={}", id);
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
        log.info("BiometricDevice updated successfully with id={}", id);
        auditService.record("UPDATE", "BIOMETRIC_DEVICE", id, actor,
                "{\"name\":\"" + safe(device.getName()) + "\",\"enabled\":" + device.isEnabled()
                        + ",\"hasPassword\":" + device.hasPassword() + "}", null);
        return response(device);
    }

    @Transactional
    public ImportApi.DeviceSyncResponse sync(String id, String actor) {
        log.debug("sync called with id={}", id);
        BiometricDevice device = requireDevice(id);
        BiometricSource source = ensureSource(device.getId(), device.getName());
        try {
            BiometricDeviceClient.DeviceResponse remote = biometricDeviceClient.fetch(device,
                    new BiometricDeviceClient.DeviceCredentials(device.getUsername(),
                            deviceCredentialsCrypto.decrypt(device.getPasswordEncrypted())));
            String checksum = sha256(remote.rawContent());
            var existing = importBatchRepository
                    .findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc(
                            source.getId(), checksum, ImportStatus.REVERSED);
            if (existing.isPresent()) {
                if (source.isAutoCreateEmployees()) {
                    remote.punches().forEach(punch -> employeeProvisioningService.resolveEmployeeId(
                            source, punch.deviceUserId(), punch.employeeName(), punch.punchedAt(), actor));
                }
                device.syncSucceeded(0, device.getLastSuccessfulPunchAt());
                biometricDeviceRepository.saveAndFlush(device);
                return new ImportApi.DeviceSyncResponse(response(device), remote.punches().size(), 0,
                        remote.punches().size(), true);
            }

            String appId = TenantContext.require();
            int size = remote.punches().size();
            String batchId = UUID.randomUUID().toString();
            String fileName = "device-sync-" + device.getId() + "-" + Instant.now().toEpochMilli() + ".json";
            int reserved = importBatchRepository.insertIfAbsent(batchId, appId, checksum, fileName,
                    source.getId(), device.getName(), ImportStatus.COMPLETED.name(), size, size, 0, actor);
            ImportBatch batch;
            if (reserved == 0) {
                batch = importBatchRepository
                        .findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc(
                                source.getId(), checksum, ImportStatus.REVERSED)
                        .orElseThrow(() -> new IllegalStateException("Reserved batch could not be loaded: " + checksum));
                device.syncSucceeded(0, device.getLastSuccessfulPunchAt());
                biometricDeviceRepository.saveAndFlush(device);
                return new ImportApi.DeviceSyncResponse(response(device), size, 0, size, true);
            }
            batch = importBatchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Reserved batch could not be loaded: " + batchId));

            int duplicates = 0;
            int imported = 0;
            Instant latest = device.getLastSuccessfulPunchAt();
            int rowNumber = 0;
            List<PunchImportEvidence> evidence = new ArrayList<>(size);
            for (var punch : remote.punches()) {
                rowNumber++;
                String employeeId = employeeProvisioningService.resolveEmployeeId(
                        source, punch.deviceUserId(), punch.employeeName(), punch.punchedAt(), actor);
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
            batch.updateCounts(size, size, 0, imported, duplicates);
            importBatchRepository.save(batch);
            device.syncSucceeded(imported, latest);
            biometricDeviceRepository.saveAndFlush(device);
            auditService.record("SYNC", "BIOMETRIC_DEVICE", id, actor,
                    "{\"received\":" + remote.punches().size() + ",\"imported\":" + imported
                            + ",\"duplicates\":" + duplicates + "}", null);
            return new ImportApi.DeviceSyncResponse(response(device), remote.punches().size(),
                    imported, duplicates, false);
        } catch (RuntimeException exception) {
            log.error("Device sync failed for id={}", id, exception);
            device.syncFailed(exception.getMessage());
            biometricDeviceRepository.saveAndFlush(device);
            auditService.record("SYNC_FAILED", "BIOMETRIC_DEVICE", id, actor,
                    "{\"message\":\"" + safe(exception.getMessage()) + "\"}", null);
            return new ImportApi.DeviceSyncResponse(response(device), 0, 0, 0, false);
        }
    }

    public List<BiometricDevice> dueDevices(Instant now) {
        log.debug("dueDevices called with now={}", now);
        return biometricDeviceRepository.findAllByOrderByNameAsc().stream().filter(device -> device.isDue(now)).toList();
    }

    private BiometricSource ensureSource(String deviceId, String deviceName) {
        BiometricSource source = biometricSourceRepository.findBySourceTypeAndNormalizedCode(
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
        if (source.getSourceType() != BiometricSource.SourceType.DEVICE) {
            throw new IllegalStateException("Device " + deviceId + " resolved to a non-device biometric source.");
        }
        return source;
    }

    private ImportApi.SourceResponse toSourceResponse(BiometricSource source) {
        return new ImportApi.SourceResponse(source.getId(), source.getName(),
                source.getSourceType().name(), source.getNormalizedCode(), source.isActive(),
                source.isAutoCreateEmployees(), source.getAutoCreateCategoryId(),
                source.getAutoCreateEmploymentType(), source.getAutoCreateActiveFromMode(),
                source.isAutoCreateEmployeeActive(),
                source.getCreatedAt());
    }

    private BiometricSource.SourceType parseSourceType(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Source type is required.", "BIO_SOURCE_TYPE_REQUIRED", HttpStatus.CONFLICT);
        }
        try {
            return BiometricSource.SourceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Unknown biometric source type.", "BIO_SOURCE_TYPE_INVALID", HttpStatus.CONFLICT);
        }
    }

    private String normalizeCode(String name) {
        return name == null ? "" : name.strip().toLowerCase().replaceAll("\\s+", "_");
    }

    private BiometricDevice requireDevice(String id) {
        return biometricDeviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Biometric device not found.", "BIO_DEVICE_NOT_FOUND"));
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
            throw new BusinessRuleException("Invalid biometric device URL; it must start with http or https.", "BIO_DEVICE_ENDPOINT_INVALID", HttpStatus.CONFLICT);
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
