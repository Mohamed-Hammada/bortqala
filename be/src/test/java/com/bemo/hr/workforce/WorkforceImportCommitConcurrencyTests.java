package com.bemo.hr.workforce;

import com.bemo.hr.PostgresIntegrationTest;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WorkforceImportCommitConcurrencyTests extends PostgresIntegrationTest {

    private final WorkforceExcelImportService importService;
    private final WorkforceImportBatchRepository batchRepository;
    private final WorkforceImportRowRepository rowRepository;
    private final WorkforceImportChangeRepository changeRepository;
    private final WorkerRepository workerRepository;
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final TenantApplicationRepository tenantApplicationRepository;

    private final List<String> createdAppIds = new ArrayList<>();
    private final List<String> createdBatchIds = new ArrayList<>();
    private final List<String> createdWorkerIds = new ArrayList<>();

    @Autowired
    WorkforceImportCommitConcurrencyTests(WorkforceExcelImportService importService,
                                          WorkforceImportBatchRepository batchRepository,
                                          WorkforceImportRowRepository rowRepository,
                                          WorkforceImportChangeRepository changeRepository,
                                          WorkerRepository workerRepository,
                                          ManualAttendanceEntryRepository attendanceRepository,
                                          TenantApplicationRepository tenantApplicationRepository) {
        this.importService = importService;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.changeRepository = changeRepository;
        this.workerRepository = workerRepository;
        this.attendanceRepository = attendanceRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
    }

    @AfterEach
    void cleanup() {
        try {
            if (!createdAppIds.isEmpty()) {
                TenantContext.set(createdAppIds.get(createdAppIds.size() - 1));
                createdBatchIds.forEach(batchId ->
                        changeRepository.findByBatchIdOrderByCreatedAtDesc(batchId)
                                .forEach(change -> changeRepository.deleteById(change.getId())));
                createdBatchIds.forEach(batchId ->
                        rowRepository.findByBatchIdOrderByRowNumberAsc(batchId)
                                .forEach(row -> rowRepository.deleteById(row.getId())));
                attendanceRepository.findAll().forEach(entry -> {
                    if (createdWorkerIds.contains(entry.getWorkerId())) attendanceRepository.deleteById(entry.getId());
                });
                createdBatchIds.forEach(batchRepository::deleteById);
            }
            workerRepository.deleteAllById(createdWorkerIds);
            tenantApplicationRepository.deleteAllById(createdAppIds);
        } finally {
            createdAppIds.clear();
            createdBatchIds.clear();
            createdWorkerIds.clear();
            TenantContext.clear();
        }
    }

    @RepeatedTest(10)
    void concurrentCommitsOfTheSameBatchApplyExactlyOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantApplication app = tenantApplicationRepository.save(
                new TenantApplication("WIMP-" + suffix, "Workforce import commit concurrency test"));
        createdAppIds.add(app.getId());
        TenantContext.set(app.getId());

        Worker worker = workerRepository.save(new Worker(
                "W-CONC-" + suffix, "Concurrency Worker", "CONTRACTOR-1", "CATEGORY-1",
                new BigDecimal("100.00"), new BigDecimal("8.0"), null,
                "MANUAL", "ACTIVE", null, null, null));
        createdWorkerIds.add(worker.getId());

        WorkforceImportBatch batch = new WorkforceImportBatch(
                "concurrent-import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "checksum-" + suffix, new byte[]{1, 2, 3}, "workerCode\tworkDate\tattendanceValue", "system");
        batch.validated(1, 1, 0);
        batch = batchRepository.save(batch);
        createdBatchIds.add(batch.getId());
        rowRepository.save(new WorkforceImportRow(batch.getId(), 1,
                worker.getCode() + "|2026-08-04|1", worker.getCode(), worker.getId(),
                "2026-08-04", BigDecimal.ONE, "VALID", null, null));

        String operationId = "WIMP-OP-" + suffix;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger applied = new AtomicInteger();
        AtomicInteger replayed = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        List<Thread> workers = List.of(
                committer(app.getId(), batch.getId(), operationId, ready, start, applied, replayed, unexpected),
                committer(app.getId(), batch.getId(), operationId, ready, start, applied, replayed, unexpected));
        workers.forEach(Thread::start);

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Thread committerThread : workers) {
            committerThread.join(TimeUnit.SECONDS.toMillis(30));
            assertThat(committerThread.isAlive()).as("commit worker must finish").isFalse();
        }

        assertThat(unexpected.get()).as("no commit worker fails unexpectedly").isNull();
        assertThat(applied.get()).as("exactly one thread applies the batch").isEqualTo(1);
        assertThat(replayed.get()).as("exactly one thread replays the idempotent result").isEqualTo(1);

        assertThat(attendanceRepository.findByWorkerIdAndWorkDate(worker.getId(), "2026-08-04"))
                .as("exactly one attendance entry is created")
                .isPresent();
        assertThat(attendanceRepository.findAll())
                .as("no duplicate attendance entry")
                .filteredOn(entry -> worker.getId().equals(entry.getWorkerId()))
                .hasSize(1);
        assertThat(changeRepository.findByBatchIdOrderByCreatedAtDesc(batch.getId()))
                .as("exactly one import change record")
                .hasSize(1);

        WorkforceImportBatch reloaded = batchRepository.findById(batch.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).as("batch status").isEqualTo("IMPORTED");
        assertThat(reloaded.getOperationId()).as("batch operation id").isEqualTo(operationId);
        assertThat(reloaded.getImportedRows()).as("imported row count").isEqualTo(1);
    }

    private Thread committer(String appId, String batchId, String operationId,
                             CountDownLatch ready, CountDownLatch start,
                             AtomicInteger applied, AtomicInteger replayed,
                             AtomicReference<Throwable> unexpected) {
        return new Thread(() -> {
            TenantContext.set(appId);
            try {
                ready.countDown();
                start.await();
                var response = importService.commit(batchId,
                        new WorkforceExcelImportService.CommitRequest(operationId, true));
                if (response.idempotentReplay()) replayed.incrementAndGet();
                else applied.incrementAndGet();
            } catch (Throwable throwable) {
                unexpected.set(throwable);
            } finally {
                TenantContext.clear();
            }
        });
    }
}
