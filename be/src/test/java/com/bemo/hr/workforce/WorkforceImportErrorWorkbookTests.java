package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkforceImportErrorWorkbookTests {
    @Test
    void createsArabicRtlErrorWorkbookWithTypedRowNumber() throws Exception {
        var batchRepository = mock(WorkforceImportBatchRepository.class);
        var rowRepository = mock(WorkforceImportRowRepository.class);
        var batch = new WorkforceImportBatch("attendance.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "checksum", new byte[]{1}, "كود العامل\tالتاريخ\tالحضور", "admin");
        batch.prePersist();
        var invalid = new WorkforceImportRow(batch.getId(), 8, "UNKNOWN | bad-date | 1", "UNKNOWN", null,
                null, null, "INVALID", "WORKER_NOT_FOUND", "العامل غير موجود");
        invalid.prePersist();
        when(batchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(rowRepository.findByBatchIdAndValidationStatusOrderByRowNumberAsc(batch.getId(), "INVALID"))
                .thenReturn(List.of(invalid));

        var service = new WorkforceExcelImportService(batchRepository, rowRepository,
                mock(WorkforceImportChangeRepository.class), mock(WorkerRepository.class),
                mock(ManualAttendanceEntryRepository.class), mock(AuditService.class));

        byte[] output = service.errorWorkbook(batch.getId());

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(output))) {
            var sheet = workbook.getSheet("أخطاء الاستيراد");
            assertThat(sheet).isNotNull();
            assertThat(sheet.isRightToLeft()).isTrue();
            assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(8);
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).contains("غير موجود");
            assertThat(sheet.getTables()).hasSize(1);
        }
    }
}
