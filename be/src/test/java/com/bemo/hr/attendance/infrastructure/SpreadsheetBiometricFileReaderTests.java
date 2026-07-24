package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpreadsheetBiometricFileReaderTests {
    private final SpreadsheetBiometricFileReader reader = new SpreadsheetBiometricFileReader("Africa/Cairo");

    @Test
    void readsArabicStructuredAttendanceRowsAsActualInAndOutPunches() throws Exception {
        byte[] file;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("الحضور");
            var header = sheet.createRow(0);
            String[] headers = {"كود الموظف", "اليوم", "الحضور الرسمي", "الانصراف الرسمي", "الحضور الفعلي", "الانصراف الفعلي"};
            for (int index = 0; index < headers.length; index++) header.createCell(index).setCellValue(headers[index]);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("EMP-001");
            row.createCell(1).setCellValue("2026-07-24");
            row.createCell(2).setCellValue("08:00");
            row.createCell(3).setCellValue("16:00");
            row.createCell(4).setCellValue("08:07");
            row.createCell(5).setCellValue("16:15");
            workbook.write(output);
            file = output.toByteArray();
        }

        var parsed = reader.read("attendance.xlsx", new ByteArrayInputStream(file));

        assertThat(parsed.totalRows()).isEqualTo(1);
        assertThat(parsed.importedRows()).isEqualTo(1);
        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).extracting(row -> row.deviceUserId()).containsOnly("EMP-001");
        assertThat(parsed.rows()).extracting(row -> row.punchedAt()).containsExactly(
                Instant.parse("2026-07-24T05:07:00Z"), Instant.parse("2026-07-24T13:15:00Z"));
    }

    @Test
    void keepsSingleActualPunchAndAcceptsBlankActualCheckout() {
        String csv = "Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
                + "EMP-002,2026-07-24,08:00,16:00,08:03,\n";

        var parsed = reader.read("attendance.csv", new ByteArrayInputStream(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThat(parsed.importedRows()).isEqualTo(1);
        assertThat(parsed.rows()).hasSize(1);
        assertThat(parsed.rows().getFirst().punchedAt()).isEqualTo(Instant.parse("2026-07-24T05:03:00Z"));
    }

    @Test
    void rejectsLegacyOrIncompleteColumnSetsWithTheNewBilingualContract() {
        String csv = "device_user_id,punched_at\n10,2026-07-24 08:00\n";

        assertThatThrownBy(() -> reader.read("attendance.csv",
                new ByteArrayInputStream(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("كود الموظف")
                .hasMessageContaining("Employee");
    }
}
