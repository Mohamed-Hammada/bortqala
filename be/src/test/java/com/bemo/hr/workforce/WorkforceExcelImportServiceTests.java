package com.bemo.hr.workforce;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkforceExcelImportServiceTests {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final int MAX_ROWS = 20_000;

    @Mock
    private WorkforceImportBatchRepository batchRepository;
    @Mock
    private WorkforceImportRowRepository rowRepository;
    @Mock
    private WorkforceImportChangeRepository changeRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private ManualAttendanceEntryRepository attendanceRepository;
    @Mock
    private com.bemo.hr.audit.application.AuditService auditService;

    @InjectMocks
    private WorkforceExcelImportService importService;

    private static byte[] workbookBytes(String[] headers, String[][] rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("import");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(1 + r);
                for (int c = 0; c < rows[r].length; c++) row.createCell(c).setCellValue(rows[r][c]);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static String encodedMapping() {
        return "workerCode\tworkerCode\nworkDate\tworkDate\nattendanceValue\tattendanceValue";
    }

    // ---------- V-17: upload() file safety ----------

    private static WorkforceImportBatch mappedBatch(String batchId, byte[] bytes) {
        WorkforceImportBatch batch = new WorkforceImportBatch("import.xlsx", XLSX, "checksum", bytes,
                "workerCode\tworkDate\tattendanceValue", "user");
        stampCreatedAt(batch);
        return batch;
    }

    private static void stampCreatedAt(WorkforceImportBatch batch) {
        try {
            Field field = WorkforceImportBatch.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(batch, Instant.now());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @BeforeEach
    void setUp() {
        TenantContext.set("test-tenant");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- V-13: validate() bounded queries ----------

    @Test
    void upload_throwsBusinessRuleException_onCorruptedFile() {
        byte[] badFile = new byte[]{0, 1, 2, 3, 4, 5};

        assertThatThrownBy(() -> importService.upload(new MockMultipartFile("file", "file.xlsx", XLSX, badFile)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("تعذر قراءة ملف البصمة.")
                .hasFieldOrPropertyWithValue("code", "EXCEL_READ_FAILED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void upload_rejectsOversizedFileBeforeAnyRepositoryRead() {
        byte[] oversized = new byte[(int) MAX_FILE_BYTES + 1];

        assertThatThrownBy(() -> importService.upload(new MockMultipartFile("file", "big.xlsx", XLSX, oversized)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("workforce.import.fileTooLarge")
                .hasFieldOrPropertyWithValue("code", "EXCEL_FILE_TOO_LARGE")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(batchRepository, never()).findByChecksum(anyString());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void upload_mapsRepositoryFailureToSafeKeyedErrorWithoutSensitiveDetails() {
        when(batchRepository.findByChecksum(anyString()))
                .thenThrow(new RuntimeException("db connection lost: secret-db-pass"));

        assertThatThrownBy(() -> importService.upload(new MockMultipartFile("file", "ok.xlsx", XLSX, new byte[]{1, 2, 3})))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("تعذر قراءة ملف البصمة.")
                .hasFieldOrPropertyWithValue("code", "EXCEL_READ_FAILED");
    }

    @Test
    void upload_rejectsWorkbookWithoutSheets() throws IOException {
        byte[] noSheets;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("temp");
            workbook.removeSheetAt(0);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                noSheets = out.toByteArray();
            }
        }
        when(batchRepository.findByChecksum(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importService.upload(new MockMultipartFile("file", "empty.xlsx", XLSX, noSheets)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("ملف Excel لا يحتوي على أوراق.");

        verify(batchRepository, never()).save(any());
    }

    // ---------- V-19: preview() bounded fetch ----------

    @Test
    void validate_queriesOnlyReferencedWorkerCodesOnceAndPersistsRows() throws IOException {
        byte[] bytes = workbookBytes(new String[]{"workerCode", "workDate", "attendanceValue"},
                new String[][]{{"W-001", "2026-08-01", "1"}, {"w-002 ", "2026-08-02", "0.5"}});
        WorkforceImportBatch batch = mappedBatch("b1", bytes);
        batch.map(encodedMapping());
        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));
        List<Worker> workers = List.of(
                new Worker("W-001", "Worker One", "c1", "cat1", BigDecimal.ZERO, BigDecimal.ONE, null, "MANUAL", "ACTIVE", null, null, null),
                new Worker("W-002", "Worker Two", "c1", "cat1", BigDecimal.ZERO, BigDecimal.ONE, null, "MANUAL", "ACTIVE", null, null, null));
        when(workerRepository.findByCodeIn(any())).thenReturn(workers);
        when(workerRepository.findByIdIn(any())).thenReturn(workers);
        when(rowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = importService.validate("b1");

        verify(workerRepository, never()).findAll();
        verify(workerRepository).findByCodeIn(argThat(codes -> codes.equals(Set.of("W-001", "W-002"))));
        verify(rowRepository).saveAll(argThat((List<WorkforceImportRow> saved) -> saved.size() == 2));
        assertThat(result.batch().status()).isEqualTo("READY");
        assertThat(result.preview()).hasSize(2);
        assertThat(result.preview().get(0).workerName()).isEqualTo("Worker One");
        assertThat(result.preview().get(0).errorCode()).isNull();
        verify(auditService).record(eq("VALIDATE"), eq("WORKFORCE_IMPORT"), eq("b1"), anyString(), contains("\"total\":2"), isNull());
    }

    // ---------- V-01: reverse() bulk reversal ----------

    @Test
    void validate_marksUnknownWorkerCodeAsInvalid() throws IOException {
        byte[] bytes = workbookBytes(new String[]{"workerCode", "workDate", "attendanceValue"},
                new String[][]{{"W-001", "2026-08-01", "1"}, {"W-999", "2026-08-02", "0.5"}});
        WorkforceImportBatch batch = mappedBatch("b1", bytes);
        batch.map(encodedMapping());
        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));
        when(workerRepository.findByCodeIn(any())).thenReturn(List.of(
                new Worker("W-001", "Worker One", "c1", "cat1", BigDecimal.ZERO, BigDecimal.ONE, null, "MANUAL", "ACTIVE", null, null, null)));
        when(workerRepository.findByIdIn(any())).thenAnswer(invocation -> List.of(
                new Worker("W-001", "Worker One", "c1", "cat1", BigDecimal.ZERO, BigDecimal.ONE, null, "MANUAL", "ACTIVE", null, null, null)));
        when(rowRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = importService.validate("b1");

        assertThat(result.preview()).hasSize(2);
        assertThat(result.preview().get(1).validationStatus()).isEqualTo("INVALID");
        assertThat(result.preview().get(1).errorCode()).isEqualTo("WORKER_NOT_FOUND");
        assertThat(result.batch().status()).isEqualTo("VALIDATED");
        verify(batchRepository).save(argThat(batchArg -> batchArg.getStatus().equals("VALIDATED")));
    }

    @Test
    void validate_rejectsDuplicateNormalizedWorkerCodesBeforePersistence() throws IOException {
        byte[] bytes = workbookBytes(new String[]{"workerCode", "workDate", "attendanceValue"},
                new String[][]{{"W-001", "2026-08-01", "1"}, {"w-001", "2026-08-02", "1"}});
        WorkforceImportBatch batch = mappedBatch("b1", bytes);
        batch.map(encodedMapping());
        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));
        when(workerRepository.findByCodeIn(any())).thenReturn(List.of(
                new Worker("W-001", "First", "c1", "cat1", BigDecimal.ZERO, BigDecimal.ONE, null, "MANUAL", "ACTIVE", null, null, null),
                new Worker("W-001", "Second", "c1", "cat1", BigDecimal.ZERO, BigDecimal.ONE, null, "MANUAL", "ACTIVE", null, null, null)));

        assertThatThrownBy(() -> importService.validate("b1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("workforce.import.duplicateWorkerCode")
                .hasFieldOrPropertyWithValue("code", "WORKFORCE_DUPLICATE_WORKER_CODE")
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(rowRepository, never()).saveAll(any());
        verify(batchRepository, never()).save(any());
        verify(auditService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void validate_rejectsWorkbookExceedingMaxRows() throws IOException {
        byte[] sparse;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("import");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("workerCode");
            header.createCell(1).setCellValue("workDate");
            header.createCell(2).setCellValue("attendanceValue");
            sheet.createRow(MAX_ROWS + 1);
            workbook.write(out);
            sparse = out.toByteArray();
        }
        WorkforceImportBatch batch = mappedBatch("b1", sparse);
        batch.map(encodedMapping());
        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> importService.validate("b1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("workforce.import.tooManyRows")
                .hasFieldOrPropertyWithValue("code", "EXCEL_TOO_MANY_ROWS")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(workerRepository, never()).findByCodeIn(any());
        verify(rowRepository, never()).saveAll(any());
    }

    @Test
    void preview_boundsTo100RowsAndFetchesOnlyPreviewWorkerIdsPreservingOrder() {
        WorkforceImportBatch batch = mappedBatch("b1", new byte[]{1});
        batch.map(encodedMapping());
        batch.validated(250, 250, 0);

        List<WorkforceImportRow> rows = new ArrayList<>();
        for (int i = 1; i <= 250; i++) {
            String workerId = (i >= 50 && i <= 55) ? null : "w" + i;
            rows.add(new WorkforceImportRow("b1", i, "raw", "W" + i, workerId, "2026-08-01", BigDecimal.ONE, "VALID", null, null));
        }
        List<Worker> first100 = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Worker worker = mock(Worker.class);
            when(worker.getId()).thenReturn("w" + i);
            when(worker.getFullName()).thenReturn("Worker " + i);
            first100.add(worker);
        }

        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));
        when(rowRepository.findByBatchIdOrderByRowNumberAsc("b1")).thenReturn(rows);
        when(workerRepository.findByIdIn(any())).thenReturn(first100);

        var result = importService.preview("b1");

        assertThat(result.preview()).hasSize(100);
        assertThat(result.preview().get(0).rowNumber()).isEqualTo(1);
        assertThat(result.preview().get(99).rowNumber()).isEqualTo(100);
        assertThat(result.preview().get(0).workerName()).isEqualTo("Worker 1");
        assertThat(result.preview().get(49).workerName()).isNull();
        verify(workerRepository, times(1)).findByIdIn(argThat(ids -> ids.size() == 94
                && ids.containsAll(Set.of("w1", "w49", "w56"))));
        verify(workerRepository, never()).findAll();
    }

    // ---------- helpers ----------

    @Test
    void reverse_bulkFetchesEntriesOnceAndRestoresState() {
        WorkforceImportBatch batch = mappedBatch("b1", new byte[]{1});
        batch.imported("op-1", 2);

        ManualAttendanceEntry createdEntry = new ManualAttendanceEntry("w1", "2026-08-01", BigDecimal.ONE,
                null, null, null, null, null, null, "EXCEL_IMPORT", "import row 1");
        ManualAttendanceEntry updatedEntry = new ManualAttendanceEntry("w2", "2026-08-02", new BigDecimal("0.5"),
                null, null, null, null, null, null, "MANUAL", "old note");
        WorkforceImportChange createdChange = new WorkforceImportChange("b1", createdEntry.getId(), true,
                null, null, null, BigDecimal.ONE);
        WorkforceImportChange updatedChange = new WorkforceImportChange("b1", updatedEntry.getId(), false,
                new BigDecimal("0.5"), "MANUAL", "old note", BigDecimal.ONE);

        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));
        when(changeRepository.findByBatchIdOrderByCreatedAtDesc("b1")).thenReturn(List.of(createdChange, updatedChange));
        when(attendanceRepository.findAllById(any())).thenReturn(List.of(createdEntry, updatedEntry));
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = importService.reverse("b1");

        verify(attendanceRepository, times(1)).findAllById(argThat((Set<String> ids) -> ids.size() == 2
                && ids.containsAll(Set.of(createdEntry.getId(), updatedEntry.getId()))));
        verify(attendanceRepository, never()).findByWorkerIdAndWorkDate(anyString(), anyString());
        assertThat(createdEntry.getAttendanceValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(createdEntry.getSource()).isEqualTo("IMPORT_REVERSAL");
        assertThat(updatedEntry.getAttendanceValue()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(updatedEntry.getSource()).isEqualTo("MANUAL");
        assertThat(updatedEntry.getNotes()).isEqualTo("old note");
        assertThat(createdChange.getReversedAt()).isNotNull();
        assertThat(updatedChange.getReversedAt()).isNotNull();
        assertThat(result.status()).isEqualTo("REVERSED");
        verify(auditService).record(eq("REVERSE"), eq("WORKFORCE_IMPORT"), eq("b1"), anyString(), contains("\"changes\":2"), isNull());
    }

    @Test
    void reverse_skipsMissingAndAlreadyReversedChanges() {
        WorkforceImportBatch batch = mappedBatch("b1", new byte[]{1});
        batch.imported("op-1", 3);

        ManualAttendanceEntry entry = new ManualAttendanceEntry("w1", "2026-08-01", BigDecimal.ONE,
                null, null, null, null, null, null, "EXCEL_IMPORT", "import row 1");
        ManualAttendanceEntry untouchedEntry = new ManualAttendanceEntry("w3", "2026-08-03", new BigDecimal("0.5"),
                null, null, null, null, null, null, "MANUAL", null);
        WorkforceImportChange presentChange = new WorkforceImportChange("b1", entry.getId(), true,
                null, null, null, BigDecimal.ONE);
        WorkforceImportChange missingChange = new WorkforceImportChange("b1", "missing-entry", true,
                null, null, null, BigDecimal.ONE);
        WorkforceImportChange alreadyReversed = new WorkforceImportChange("b1", untouchedEntry.getId(), false,
                new BigDecimal("0.5"), "MANUAL", null, BigDecimal.ONE);
        alreadyReversed.reversed();

        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));
        when(changeRepository.findByBatchIdOrderByCreatedAtDesc("b1"))
                .thenReturn(List.of(presentChange, missingChange, alreadyReversed));
        when(attendanceRepository.findAllById(any())).thenReturn(List.of(entry));
        when(batchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        importService.reverse("b1");

        assertThat(presentChange.getReversedAt()).isNotNull();
        assertThat(missingChange.getReversedAt()).isNull();
        assertThat(alreadyReversed.getReversedAt()).isNotNull();
        assertThat(entry.getSource()).isEqualTo("IMPORT_REVERSAL");
        assertThat(untouchedEntry.getAttendanceValue()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(untouchedEntry.getSource()).isEqualTo("MANUAL");
        verify(auditService).record(eq("REVERSE"), eq("WORKFORCE_IMPORT"), eq("b1"), anyString(), contains("\"changes\":3"), isNull());
    }

    @Test
    void reverse_secondCallIsIdempotentWithoutRepositoryWrites() {
        WorkforceImportBatch batch = mappedBatch("b1", new byte[]{1});
        batch.imported("op-1", 2);
        batch.reversed("user");
        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));

        var result = importService.reverse("b1");

        assertThat(result.status()).isEqualTo("REVERSED");
        verify(changeRepository, never()).findByBatchIdOrderByCreatedAtDesc(anyString());
        verify(attendanceRepository, never()).findAllById(any());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void reverse_rejectsNonImportedBatch() {
        WorkforceImportBatch batch = mappedBatch("b1", new byte[]{1});
        batch.map(encodedMapping());
        batch.validated(2, 1, 1);
        when(batchRepository.findById("b1")).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> importService.reverse("b1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("يمكن التراجع عن عملية منفذة فقط.");

        verify(changeRepository, never()).findByBatchIdOrderByCreatedAtDesc(anyString());
        verify(attendanceRepository, never()).findAllById(any());
    }
}
