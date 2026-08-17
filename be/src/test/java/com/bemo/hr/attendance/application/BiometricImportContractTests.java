package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.domain.BiometricSource;
import com.bemo.hr.attendance.domain.ImportStatus;
import com.bemo.hr.attendance.infrastructure.*;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BiometricImportContractTests {


    private static final String CSV = "Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
            + "EMP-101,2026-07-24,08:00,16:00,08:07,16:15\n";
    private final BiometricImportService biometricImportService;
    private final BiometricDeviceSyncService biometricDeviceSyncService;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final ImportRowErrorRepository importRowErrorRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BiometricSourceRepository biometricSourceRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final DeviceCredentialsCrypto deviceCredentialsCrypto;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final List<String> createdBatches = new ArrayList<>();
    private final List<String> createdDevices = new ArrayList<>();
    private final List<String> createdSources = new ArrayList<>();
    private final List<String> createdApps = new ArrayList<>();

    @Autowired
    BiometricImportContractTests(BiometricImportService biometricImportService,
                                 BiometricDeviceSyncService biometricDeviceSyncService,
                                 ImportBatchRepository importBatchRepository,
                                 PunchRecordRepository punchRecordRepository,
                                 ImportRowErrorRepository importRowErrorRepository,
                                 BiometricDeviceRepository biometricDeviceRepository,
                                 BiometricSourceRepository biometricSourceRepository,
                                 TenantApplicationRepository tenantApplicationRepository,
                                 DeviceCredentialsCrypto deviceCredentialsCrypto,
                                 ObjectMapper objectMapper,
                                 PlatformTransactionManager transactionManager) {
        this.biometricImportService = biometricImportService;
        this.biometricDeviceSyncService = biometricDeviceSyncService;
        this.importBatchRepository = importBatchRepository;
        this.punchRecordRepository = punchRecordRepository;
        this.importRowErrorRepository = importRowErrorRepository;
        this.biometricDeviceRepository = biometricDeviceRepository;
        this.biometricSourceRepository = biometricSourceRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.deviceCredentialsCrypto = deviceCredentialsCrypto;
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanup() {
        try {
            String app = createdApps.isEmpty() ? null : createdApps.get(createdApps.size() - 1);
            if (app != null) {
                TenantContext.set(app);
                tx.executeWithoutResult(status -> {
                    for (String batch : createdBatches) {
                        punchRecordRepository.deleteByBatchId(batch);
                        importRowErrorRepository.deleteByBatchId(batch);
                    }
                    importBatchRepository.deleteAllById(createdBatches);
                    biometricDeviceRepository.deleteAllById(createdDevices);
                    biometricSourceRepository.deleteAllById(createdSources);
                });
            }
            tenantApplicationRepository.deleteAllById(createdApps);
        } finally {
            TenantContext.clear();
        }
    }

    private TenantApplication app() {
        var created = tenantApplicationRepository.save(
                new TenantApplication("APP-IMP-" + UUID.randomUUID().toString().substring(0, 6),
                        "Import Test"));
        createdApps.add(created.getId());
        return created;
    }

    private MockMultipartFile csv(String fileName, String content) {
        return new MockMultipartFile("file", fileName, "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private String fileSource(String name) {
        var source = biometricSourceRepository.findBySourceTypeAndNormalizedCode(
                        BiometricSource.SourceType.FILE_DEVICE, name.strip().toLowerCase().replaceAll("\\s+", "_"))
                .orElseGet(() -> biometricSourceRepository.save(new BiometricSource(
                        BiometricSource.SourceType.FILE_DEVICE, name,
                        name.strip().toLowerCase().replaceAll("\\s+", "_"))));
        createdSources.add(source.getId());
        return source.getId();
    }

    @Test
    void previewParsesTheFileWithoutPersistingAnything() {
        var app = app();
        TenantContext.set(app.getId());

        var preview = biometricImportService.preview(csv("attendance.csv", CSV));

        assertThat(preview.checksum()).hasSize(64);
        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.importedRows()).isEqualTo(1);
        assertThat(preview.errorRows()).isZero();
        assertThat(preview.rows()).hasSize(2);
        assertThat(preview.rows()).extracting(row -> row.deviceUserId()).containsOnly("EMP-101");
        assertThat(importBatchRepository.count()).isZero();
        assertThat(punchRecordRepository.count()).isZero();
    }

    @Test
    void uploadingTheSameFileTwiceCreatesNoDuplicatePunches() {
        var app = app();
        TenantContext.set(app.getId());
        String sourceId = fileSource("بوابة المصنع");

        var first = biometricImportService.importFile(csv("attendance.csv", CSV), sourceId, "tester");
        createdBatches.add(first.id());
        var second = biometricImportService.importFile(csv("attendance.csv", CSV), sourceId, "tester");

        assertThat(first.duplicate()).isFalse();
        assertThat(first.importedRows()).isEqualTo(1);
        assertThat(first.validRows()).isEqualTo(1);
        assertThat(first.newPunches()).isEqualTo(2);
        assertThat(first.duplicatePunches()).isZero();
        assertThat(first.totalRows()).isEqualTo(1);
        assertThat(second.duplicate()).isTrue();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.status()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(punchRecordRepository.countByBatchId(first.id())).isEqualTo(2);
    }

    @Test
    void reverseDeletesPunchesAndErrorsIsIdempotentAndAllowsReimportOfSameFile() {
        var app = app();
        TenantContext.set(app.getId());
        String sourceId = fileSource("بوابة المصنع");

        var uploaded = biometricImportService.importFile(csv("attendance.csv", CSV), sourceId, "tester");
        createdBatches.add(uploaded.id());
        assertThat(punchRecordRepository.countByBatchId(uploaded.id())).isEqualTo(2);

        var reversed = biometricImportService.reverse(uploaded.id(), "tester");
        assertThat(reversed.status()).isEqualTo(ImportStatus.REVERSED);
        assertThat(reversed.newPunches()).isZero();
        assertThat(reversed.duplicatePunches()).isZero();
        assertThat(punchRecordRepository.countByBatchId(uploaded.id())).isZero();
        assertThat(importRowErrorRepository.findByBatchIdOrderByRowNumber(uploaded.id())).isEmpty();

        var again = biometricImportService.reverse(uploaded.id(), "tester");
        assertThat(again.status()).isEqualTo(ImportStatus.REVERSED);
        assertThat(punchRecordRepository.countByBatchId(uploaded.id())).isZero();

        var reimport = biometricImportService.importFile(csv("attendance.csv", CSV), sourceId, "tester");
        createdBatches.add(reimport.id());
        assertThat(reimport.duplicate()).isFalse();
        assertThat(reimport.id()).isNotEqualTo(uploaded.id());
        assertThat(reimport.status()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(punchRecordRepository.countByBatchId(reimport.id())).isEqualTo(2);
    }

    @Test
    void devicePasswordsAreEncryptedAtRestAndNeverReturned() throws Exception {
        var app = app();
        TenantContext.set(app.getId());

        var request = new ImportApi.DeviceRequest("بوابة ١", "http://192.168.1.50/api/punches", true, 15,
                "admin", "sup3r-secret");
        var created = biometricDeviceSyncService.create(request, "tester");
        createdDevices.add(created.id());

        assertThat(created.username()).isEqualTo("admin");
        assertThat(created.hasPassword()).isTrue();
        assertThat(created.id()).isNotBlank();

        var stored = biometricDeviceRepository.findById(created.id()).orElseThrow();
        assertThat(stored.getPasswordEncrypted()).isNotBlank();
        assertThat(stored.getPasswordEncrypted()).isNotEqualTo("sup3r-secret");
        assertThat(deviceCredentialsCrypto.decrypt(stored.getPasswordEncrypted())).isEqualTo("sup3r-secret");

        String json = objectMapper.writeValueAsString(created);
        assertThat(json).doesNotContain("sup3r-secret");

        var kept = biometricDeviceSyncService.update(created.id(),
                new ImportApi.DeviceRequest(created.name(), created.endpointUrl(), true, 15, "admin", null), "tester");
        assertThat(kept.hasPassword()).isTrue();
        assertThat(biometricDeviceRepository.findById(created.id()).orElseThrow().getPasswordEncrypted())
                .isEqualTo(stored.getPasswordEncrypted());

        var changed = biometricDeviceSyncService.update(created.id(),
                new ImportApi.DeviceRequest(created.name(), created.endpointUrl(), true, 15, "admin", "new-pass"), "tester");
        assertThat(changed.hasPassword()).isTrue();
        assertThat(deviceCredentialsCrypto.decrypt(
                biometricDeviceRepository.findById(created.id()).orElseThrow().getPasswordEncrypted()))
                .isEqualTo("new-pass");
    }

    @Test
    void cryptoRoundTripRejectsWrongKey() {
        String encrypted = deviceCredentialsCrypto.encrypt("roundtrip");
        assertThat(encrypted).isNotEqualTo("roundtrip");
        assertThat(deviceCredentialsCrypto.decrypt(encrypted)).isEqualTo("roundtrip");
        assertThatThrownBy(() -> new DeviceCredentialsCrypto(
                "ZGV2aWNlLWNyZWRlbnRpYWxzLTMyLWJ5dGVzLWtleSE=".replace("S", "T"))
                .decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
        assertThat(deviceCredentialsCrypto.encrypt("  ")).isNull();
    }
}
