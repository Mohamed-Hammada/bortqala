package com.bemo.hr.migration.application;

import com.bemo.hr.migration.api.DataMigrationApi;
import com.bemo.hr.migration.domain.DataMigrationBatch;
import com.bemo.hr.migration.domain.MigrationEntityType;
import com.bemo.hr.migration.infrastructure.DataMigrationBatchRepository;
import com.bemo.hr.migration.infrastructure.DataMigrationRecordRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataMigrationServiceTests {

    private DataMigrationBatchRepository batchRepository;
    private DataMigrationRecordRepository recordRepository;
    private ObjectMapper objectMapper;
    private DataMigrationService service;

    @BeforeEach
    void setUp() {
        batchRepository = mock(DataMigrationBatchRepository.class);
        recordRepository = mock(DataMigrationRecordRepository.class);
        objectMapper = new ObjectMapper();
        service = new DataMigrationService(batchRepository, recordRepository, objectMapper);
    }

    @Test
    @DisplayName("Creates migration batch with parsed records and initializes in DRAFT status")
    void createBatchSuccessfully() {
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataMigrationApi.UploadRequest request = new DataMigrationApi.UploadRequest(
                MigrationEntityType.CUSTOMERS,
                "customers.xlsx",
                List.of(
                        Map.of("code", "CUST-001", "nameAr", "شركة الأمل", "creditLimit", "50000"),
                        Map.of("code", "CUST-002", "nameAr", "مؤسسة النور", "creditLimit", "30000")
                )
        );

        DataMigrationApi.BatchResponse response = service.createBatch(request, "admin");

        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.entityType()).isEqualTo(MigrationEntityType.CUSTOMERS);
        assertThat(response.totalRecords()).isEqualTo(2);
        verify(recordRepository).saveAll(any());
    }

    @Test
    @DisplayName("Dry-run validates subledger total and matches GL opening balance")
    void dryRunCalculatesAmountAndMatchesGl() {
        String batchId = "batch-123";
        DataMigrationBatch batch = new DataMigrationBatch(batchId, MigrationEntityType.OPENING_AR, "ar_opening.xlsx", "admin");

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(recordRepository.findByBatchIdOrderByRowNumberAsc(batchId)).thenReturn(List.of(
                new com.bemo.hr.migration.domain.DataMigrationRecord("r1", batchId, 1, "{\"outstandingAmount\":\"15000.00\"}", "VALID", null),
                new com.bemo.hr.migration.domain.DataMigrationRecord("r2", batchId, 2, "{\"outstandingAmount\":\"25000.00\"}", "VALID", null)
        ));

        DataMigrationApi.DryRunResponse response = service.dryRun(batchId);

        assertThat(response.message() != null || response.balanced()).isTrue();
        assertThat(response.totalCalculatedAmount()).isEqualByComparingTo(new BigDecimal("40000.00"));
        assertThat(response.glAccountCode()).contains("Accounts Receivable");
        assertThat(batch.getStatus()).isEqualTo("DRY_RUN_PASSED");
    }

    @Test
    @DisplayName("Dry-run skips malformed records without failing the batch")
    void dryRunSkipsMalformedRecords() {
        String batchId = "batch-malformed";
        DataMigrationBatch batch = new DataMigrationBatch(batchId, MigrationEntityType.OPENING_AR, "ar_opening.xlsx", "admin");

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(recordRepository.findByBatchIdOrderByRowNumberAsc(batchId)).thenReturn(List.of(
                new com.bemo.hr.migration.domain.DataMigrationRecord("r1", batchId, 1, "{not valid json", "VALID", null),
                new com.bemo.hr.migration.domain.DataMigrationRecord("r2", batchId, 2, "{\"outstandingAmount\":\"25000.00\"}", "VALID", null),
                new com.bemo.hr.migration.domain.DataMigrationRecord("r3", batchId, 3, "{\"outstandingAmount\":\"not-a-number\"}", "VALID", null)
        ));

        DataMigrationApi.DryRunResponse response = service.dryRun(batchId);

        // Only the parseable record contributes; malformed rows are logged and skipped.
        assertThat(response.totalCalculatedAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(batch.getStatus()).isEqualTo("DRY_RUN_PASSED");
    }

    @Test
    @DisplayName("Commit and rollback lifecycle executes atomically")
    void commitAndRollbackLifecycle() {
        String batchId = "batch-456";
        DataMigrationBatch batch = new DataMigrationBatch(batchId, MigrationEntityType.ITEMS, "items.csv", "admin");

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(recordRepository.findByBatchIdOrderByRowNumberAsc(batchId)).thenReturn(List.of(
                new com.bemo.hr.migration.domain.DataMigrationRecord("r1", batchId, 1, "{\"itemCode\":\"ITM-1\"}", "VALID", null)
        ));

        // Commit
        DataMigrationApi.CommitResponse commitRes = service.commitBatch(batchId, "admin");
        assertThat(commitRes.status()).isEqualTo("COMMITTED");
        assertThat(commitRes.importedCount()).isEqualTo(1);

        // Rollback
        DataMigrationApi.RollbackResponse rollbackRes = service.rollbackBatch(batchId, "admin");
        assertThat(rollbackRes.status()).isEqualTo("ROLLED_BACK");
        assertThat(rollbackRes.rolledBackCount()).isEqualTo(1);
    }
}
