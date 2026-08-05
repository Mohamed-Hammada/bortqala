package com.bemo.hr.attendance;

import com.bemo.hr.PostgresIntegrationTest;
import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.application.BiometricDeviceClient;
import com.bemo.hr.attendance.application.BiometricDeviceSyncService;
import com.bemo.hr.attendance.application.BiometricImportService;
import com.bemo.hr.attendance.domain.BiometricDevice;
import com.bemo.hr.attendance.domain.BiometricSource;
import com.bemo.hr.attendance.domain.ImportBatch;
import com.bemo.hr.attendance.infrastructure.BiometricDeviceRepository;
import com.bemo.hr.attendance.infrastructure.BiometricSourceRepository;
import com.bemo.hr.attendance.infrastructure.DeviceCredentialsCrypto;
import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PunchSourceIdentityConcurrencyTests extends PostgresIntegrationTest {

    private static final Instant PUNCH_TIME = Instant.parse("2026-08-04T06:00:00Z");

    private final BiometricDeviceSyncService syncService;
    private final BiometricImportService importService;
    private final PunchRecordRepository punchRecordRepository;
    private final ImportBatchRepository importBatchRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BiometricSourceRepository biometricSourceRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final DeviceCredentialsCrypto deviceCredentialsCrypto;
    private final StubDeviceClientConfiguration stubDeviceClientConfiguration;

    private final List<String> createdDevices = new ArrayList<>();
    private final List<String> createdSources = new ArrayList<>();
    private final List<String> createdApps = new ArrayList<>();

    @Autowired
    PunchSourceIdentityConcurrencyTests(BiometricDeviceSyncService syncService,
                                        BiometricImportService importService,
                                        PunchRecordRepository punchRecordRepository,
                                        ImportBatchRepository importBatchRepository,
                                        BiometricDeviceRepository biometricDeviceRepository,
                                        BiometricSourceRepository biometricSourceRepository,
                                        TenantApplicationRepository tenantApplicationRepository,
                                        DeviceCredentialsCrypto deviceCredentialsCrypto,
                                        StubDeviceClientConfiguration stubDeviceClientConfiguration) {
        this.syncService = syncService;
        this.importService = importService;
        this.punchRecordRepository = punchRecordRepository;
        this.importBatchRepository = importBatchRepository;
        this.biometricDeviceRepository = biometricDeviceRepository;
        this.biometricSourceRepository = biometricSourceRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.deviceCredentialsCrypto = deviceCredentialsCrypto;
        this.stubDeviceClientConfiguration = stubDeviceClientConfiguration;
    }

    @AfterEach
    void cleanup() {
        try {
            String app = createdApps.isEmpty() ? null : createdApps.get(createdApps.size() - 1);
            if (app != null) {
                TenantContext.set(app);
                importBatchRepository.findAll().forEach(batch -> {
                    punchRecordRepository.deleteByBatchId(batch.getId());
                    importBatchRepository.deleteById(batch.getId());
                });
                biometricDeviceRepository.deleteAllById(createdDevices);
                biometricSourceRepository.deleteAllById(createdSources);
            }
            tenantApplicationRepository.deleteAllById(createdApps);
        } finally {
            createdApps.clear();
            createdDevices.clear();
            createdSources.clear();
            TenantContext.clear();
        }
    }

    private String app() {
        var created = tenantApplicationRepository.save(
                new TenantApplication("APP-PUNCH-" + UUID.randomUUID().toString().substring(0, 6),
                        "Punch Source Concurrency Test"));
        createdApps.add(created.getId());
        return created.getId();
    }

    private BiometricDevice device(String appId, String name) {
        BiometricDevice device = biometricDeviceRepository.save(
                new BiometricDevice(name, "http://192.168.1.50/api/punches", true, 15));
        device.setCredentials("admin", deviceCredentialsCrypto.encrypt("test-password"));
        biometricDeviceRepository.saveAndFlush(device);
        createdDevices.add(device.getId());
        return device;
    }

    private String fileSource(String appId, String name) {
        var source = biometricSourceRepository.findBySourceTypeAndNormalizedCode(
                        BiometricSource.SourceType.FILE_DEVICE, name.strip().toLowerCase().replaceAll("\\s+", "_"))
                .orElseGet(() -> biometricSourceRepository.save(new BiometricSource(
                        BiometricSource.SourceType.FILE_DEVICE, name,
                        name.strip().toLowerCase().replaceAll("\\s+", "_"))));
        createdSources.add(source.getId());
        return source.getId();
    }

    @Test
    void concurrentSyncsOfTheSamePunchCompleteSuccessfullyAndStoreItExactlyOnce() throws Exception {
        String appId = app();
        TenantContext.set(appId);
        BiometricDevice device = device(appId, "Gate " + UUID.randomUUID().toString().substring(0, 4));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<ImportApi.DeviceSyncResponse> first = new AtomicReference<>();
        AtomicReference<ImportApi.DeviceSyncResponse> second = new AtomicReference<>();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        List<Thread> threads = List.of(
                syncer(appId, device.getId(), ready, start, first, unexpected),
                syncer(appId, device.getId(), ready, start, second, unexpected));
        threads.forEach(Thread::start);

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(30));
            assertThat(thread.isAlive()).as("sync thread must finish").isFalse();
        }

        assertThat(unexpected.get()).as("no sync worker fails unexpectedly").isNull();
        assertThat(first.get().device().lastStatus())
                .as("first sync completes successfully (%s)", first.get().device().lastMessage())
                .isEqualTo("SUCCESS");
        assertThat(second.get().device().lastStatus())
                .as("second sync completes successfully (%s)", second.get().device().lastMessage())
                .isEqualTo("SUCCESS");

        assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(
                deviceSourceId(device.getId()), "U-1", PUNCH_TIME))
                .as("exactly one punch row is stored for the device source")
                .isEqualTo(1);
    }

    @Test
    void twoDifferentDevicesCanStoreTheSameUserAndTimestamp() {
        String appId = app();
        TenantContext.set(appId);
        BiometricDevice deviceA = device(appId, "Gate A");
        BiometricDevice deviceB = device(appId, "Gate B");

        ImportApi.DeviceSyncResponse syncA = syncService.sync(deviceA.getId(), "tester");
        ImportApi.DeviceSyncResponse syncB = syncService.sync(deviceB.getId(), "tester");

        assertThat(syncA.device().lastStatus())
                .as("device A sync status (%s)", syncA.device().lastMessage())
                .isEqualTo("SUCCESS");
        assertThat(syncB.device().lastStatus())
                .as("device B sync status (%s)", syncB.device().lastMessage())
                .isEqualTo("SUCCESS");
        assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(
                deviceSourceId(deviceA.getId()), "U-1", PUNCH_TIME)).isEqualTo(1);
        assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(
                deviceSourceId(deviceB.getId()), "U-1", PUNCH_TIME)).isEqualTo(1);
        assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(
                "missing", "U-1", PUNCH_TIME)).isZero();
    }

    @Test
    void reverseOriginalThenDuplicateRemovesPunchAfterLastEvidence() {
        String appId = app();
        TenantContext.set(appId);
        BiometricDevice device = device(appId, "Gate " + UUID.randomUUID().toString().substring(0, 4));

        syncService.sync(device.getId(), "tester");
        syncService.sync(device.getId(), "tester");
        List<String> batches = syncBatches(device.getId());
        assertThat(batches).as("two syncs produce two batches").hasSize(2);
        String sourceId = deviceSourceId(device.getId());

        importService.reverse(batches.get(0), "tester");
        assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(sourceId, "U-1", PUNCH_TIME))
                .as("punch survives while the duplicate batch still claims it").isEqualTo(1);

        importService.reverse(batches.get(1), "tester");
        assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(sourceId, "U-1", PUNCH_TIME))
                .as("punch is removed once the last evidence is reversed").isZero();
    }

    @Test
    void reverseDuplicateThenOriginalRemovesPunchAfterLastEvidence() {
        String appId = app();
        TenantContext.set(appId);
        BiometricDevice device = device(appId, "Gate " + UUID.randomUUID().toString().substring(0, 4));

        syncService.sync(device.getId(), "tester");
        syncService.sync(device.getId(), "tester");
        List<String> batches = syncBatches(device.getId());
        assertThat(batches).as("two syncs produce two batches").hasSize(2);
        String sourceId = deviceSourceId(device.getId());

        importService.reverse(batches.get(1), "tester");
        assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(sourceId, "U-1", PUNCH_TIME))
                .as("punch survives while the original batch still claims it").isEqualTo(1);

        importService.reverse(batches.get(0), "tester");
        assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(sourceId, "U-1", PUNCH_TIME))
                .as("punch is removed once the last evidence is reversed").isZero();
    }

    @Test
    void overlappingFilesFromTheSameSourceDoNotDuplicatePunches() {
        String appId = app();
        TenantContext.set(appId);
        String sourceId = fileSource(appId, "بوابة المصنع");

        String fileA = "Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
                + "EMP-101,2026-08-04,08:00,16:00,08:07,16:15\n"
                + "EMP-202,2026-08-04,08:00,16:00,08:10,16:00\n";
        String fileB = "Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
                + "EMP-101,2026-08-04,08:00,16:00,08:07,16:15\n";

        var previewA = importService.preview(csv("a.csv", fileA));
        var previewB = importService.preview(csv("b.csv", fileB));
        var distinct = new java.util.HashSet<String>();
        previewA.rows().forEach(row -> distinct.add(row.deviceUserId() + "|" + row.punchedAt()));
        previewB.rows().forEach(row -> distinct.add(row.deviceUserId() + "|" + row.punchedAt()));

        var first = importService.importFile(csv("a.csv", fileA), sourceId, "tester");
        var second = importService.importFile(csv("b.csv", fileB), sourceId, "tester");

        assertThat(first.duplicate()).isFalse();
        assertThat(second.duplicate()).as("different files are separate batches").isFalse();
        assertThat(first.id()).isNotEqualTo(second.id());

        long stored = punchRecordRepository.findInRange(
                        Instant.parse("2026-08-04T00:00:00Z"), Instant.parse("2026-08-05T00:00:00Z"))
                .stream()
                .filter(punch -> punch.getSourceId().equals(sourceId))
                .map(punch -> punch.getDeviceUserId() + "|" + punch.getPunchedAt())
                .distinct()
                .count();
        assertThat(stored).as("overlapping punches across files are stored exactly once")
                .isEqualTo(distinct.size());
    }

    @Test
    void concurrentSameFileUploadReturnsOneBatchAndOneReplay() throws Exception {
        String appId = app();
        TenantContext.set(appId);
        String sourceId = fileSource(appId, "بوابة الشحن");

        String content = "Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
                + "EMP-301,2026-08-04,08:00,16:00,08:12,16:20\n";

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<ImportApi.BatchResponse> first = new AtomicReference<>();
        AtomicReference<ImportApi.BatchResponse> second = new AtomicReference<>();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        List<Thread> threads = List.of(
                uploader(appId, sourceId, csv("same.csv", content), ready, start, first, unexpected),
                uploader(appId, sourceId, csv("same.csv", content), ready, start, second, unexpected));
        threads.forEach(Thread::start);

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(30));
            assertThat(thread.isAlive()).as("upload thread must finish").isFalse();
        }

        assertThat(unexpected.get()).as("no upload worker fails unexpectedly").isNull();
        assertThat(first.get().id()).as("both responses resolve to the same reserved batch")
                .isEqualTo(second.get().id());
        assertThat(first.get().duplicate() ^ second.get().duplicate())
                .as("exactly one caller wins the batch reservation").isTrue();

        long batches = importBatchRepository.findAll().stream()
                .filter(batch -> batch.getSourceId().equals(sourceId))
                .count();
        assertThat(batches).as("concurrent identical uploads create exactly one batch").isEqualTo(1);

        long stored = punchRecordRepository.findInRange(
                        Instant.parse("2026-08-04T00:00:00Z"), Instant.parse("2026-08-05T00:00:00Z"))
                .stream()
                .filter(punch -> punch.getSourceId().equals(sourceId))
                .count();
        assertThat(stored).as("the winning upload stores the punch exactly once").isEqualTo(1);
    }

    @Test
    void concurrentDeviceSyncWithIdenticalRawContentReturnsOneBatch() throws Exception {
        stubDeviceClientConfiguration.deterministicContent.set(true);
        try {
            String appId = app();
            TenantContext.set(appId);
            BiometricDevice device = device(appId, "Gate " + UUID.randomUUID().toString().substring(0, 4));

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<ImportApi.DeviceSyncResponse> first = new AtomicReference<>();
            AtomicReference<ImportApi.DeviceSyncResponse> second = new AtomicReference<>();
            AtomicReference<Throwable> unexpected = new AtomicReference<>();

            List<Thread> threads = List.of(
                    syncer(appId, device.getId(), ready, start, first, unexpected),
                    syncer(appId, device.getId(), ready, start, second, unexpected));
            threads.forEach(Thread::start);

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Thread thread : threads) {
                thread.join(TimeUnit.SECONDS.toMillis(30));
                assertThat(thread.isAlive()).as("sync thread must finish").isFalse();
            }

            assertThat(unexpected.get()).as("no sync worker fails unexpectedly").isNull();
            assertThat(first.get().device().lastStatus())
                    .as("first sync completes successfully (%s)", first.get().device().lastMessage())
                    .isEqualTo("SUCCESS");
            assertThat(second.get().device().lastStatus())
                    .as("second sync completes successfully (%s)", second.get().device().lastMessage())
                    .isEqualTo("SUCCESS");
            assertThat(first.get().duplicateBatch() ^ second.get().duplicateBatch())
                    .as("exactly one sync reserves the identical raw content").isTrue();

            long batches = importBatchRepository.findAll().stream()
                    .filter(batch -> batch.getFileName() != null
                            && batch.getFileName().startsWith("device-sync-" + device.getId() + "-"))
                    .count();
            assertThat(batches).as("identical concurrent device syncs create exactly one batch").isEqualTo(1);
            assertThat(punchRecordRepository.countBySourceIdAndDeviceUserIdAndPunchedAt(
                    deviceSourceId(device.getId()), "U-1", PUNCH_TIME))
                    .as("the winning sync stores the punch exactly once")
                    .isEqualTo(1);
        } finally {
            stubDeviceClientConfiguration.deterministicContent.set(false);
        }
    }

    private String deviceSourceId(String deviceId) {
        return biometricSourceRepository.findBySourceTypeAndNormalizedCode(
                        BiometricSource.SourceType.DEVICE, deviceId)
                .orElseThrow().getId();
    }

    private List<String> syncBatches(String deviceId) {
        return importBatchRepository.findAll().stream()
                .filter(batch -> batch.getFileName() != null
                        && batch.getFileName().startsWith("device-sync-" + deviceId + "-"))
                .sorted(Comparator.comparing(batch -> batch.getImportedAt()))
                .map(batch -> batch.getId())
                .toList();
    }

    private Thread syncer(String appId, String deviceId, CountDownLatch ready, CountDownLatch start,
                          AtomicReference<ImportApi.DeviceSyncResponse> result,
                          AtomicReference<Throwable> unexpected) {
        return new Thread(() -> {
            TenantContext.set(appId);
            try {
                ready.countDown();
                start.await();
                result.set(syncService.sync(deviceId, "tester"));
            } catch (Throwable throwable) {
                unexpected.set(throwable);
            } finally {
                TenantContext.clear();
            }
        });
    }

    private Thread uploader(String appId, String sourceId, MockMultipartFile file,
                            CountDownLatch ready, CountDownLatch start,
                            AtomicReference<ImportApi.BatchResponse> result,
                            AtomicReference<Throwable> unexpected) {
        return new Thread(() -> {
            TenantContext.set(appId);
            try {
                ready.countDown();
                start.await();
                result.set(importService.importFile(file, sourceId, "tester"));
            } catch (Throwable throwable) {
                unexpected.set(throwable);
            } finally {
                TenantContext.clear();
            }
        });
    }

    private MockMultipartFile csv(String fileName, String content) {
        return new MockMultipartFile("file", fileName, "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class StubDeviceClientConfiguration {
        final AtomicBoolean deterministicContent = new AtomicBoolean(false);

        @Bean
        @Primary
        BiometricDeviceClient stubDeviceClient() {
            return new BiometricDeviceClient() {
                @Override
                public DeviceResponse fetch(BiometricDevice device, DeviceCredentials credentials) {
                    String content = deterministicContent.get()
                            ? device.getId() + "|U-1|" + PUNCH_TIME
                            : device.getId() + "|" + UUID.randomUUID() + "|U-1|" + PUNCH_TIME;
                    return new DeviceResponse(content.getBytes(StandardCharsets.UTF_8),
                            List.of(new DevicePunch("U-1", "Punch User", PUNCH_TIME, "U-1|" + PUNCH_TIME)));
                }
            };
        }
    }
}
