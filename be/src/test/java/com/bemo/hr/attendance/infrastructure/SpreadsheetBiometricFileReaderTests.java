package com.bemo.hr.attendance.infrastructure;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class SpreadsheetBiometricFileReaderTests {
    private final ZoneId zone = ZoneId.of("Africa/Cairo");
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
                LocalDate.of(2026, 7, 24).atTime(8, 7).atZone(zone).toInstant(),
                LocalDate.of(2026, 7, 24).atTime(16, 15).atZone(zone).toInstant());
    }

    @Test
    void keepsSingleActualPunchAndAcceptsBlankActualCheckout() {
        String csv = "Employee code,Day,Official check-in,Official check-out,Actual check-in,Actual check-out\n"
                + "EMP-002,2026-07-24,08:00,16:00,08:03,\n";

        var parsed = reader.read("attendance.csv", new ByteArrayInputStream(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThat(parsed.importedRows()).isEqualTo(1);
        assertThat(parsed.rows()).hasSize(1);
        assertThat(parsed.rows().get(0).punchedAt()).isEqualTo(LocalDate.of(2026, 7, 24).atTime(8, 3).atZone(zone).toInstant());
    }

    @Test
    void acceptsUnformattedExcelSerialAndDayMonthYearDates() throws Exception {
        byte[] file;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("attendance");
            var header = sheet.createRow(0);
            String[] headers = {"Employee code", "Day", "Official check-in", "Official check-out", "Actual check-in", "Actual check-out"};
            for (int index = 0; index < headers.length; index++) header.createCell(index).setCellValue(headers[index]);
            var numericDate = sheet.createRow(1);
            numericDate.createCell(0).setCellValue("EMP-003");
            numericDate.createCell(1).setCellValue(org.apache.poi.ss.usermodel.DateUtil.getExcelDate(java.time.LocalDate.of(2026, 7, 29).atStartOfDay()));
            numericDate.createCell(2).setCellValue("08:00");
            numericDate.createCell(3).setCellValue("16:00");
            numericDate.createCell(4).setCellValue("08:05");
            numericDate.createCell(5).setCellValue("16:10");
            var textualDate = sheet.createRow(2);
            textualDate.createCell(0).setCellValue("EMP-004");
            textualDate.createCell(1).setCellValue("30-07-2026");
            textualDate.createCell(2).setCellValue("08:00");
            textualDate.createCell(3).setCellValue("16:00");
            textualDate.createCell(4).setCellValue("08:00");
            textualDate.createCell(5).setCellValue("16:00");
            workbook.write(output);
            file = output.toByteArray();
        }

        var parsed = reader.read("attendance.xlsx", new ByteArrayInputStream(file));

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.importedRows()).isEqualTo(2);
        assertThat(parsed.rows()).extracting(row -> row.punchedAt()).contains(
                LocalDate.of(2026, 7, 29).atTime(8, 5).atZone(zone).toInstant(),
                LocalDate.of(2026, 7, 30).atTime(8, 0).atZone(zone).toInstant());
    }

    @Test
    void readsTheSimpleCsvTemplateContract() {
        String csv = "device_user_id,punched_at,employee_name\n10,2026-07-24 08:00:00,Ahmed\n";

        var parsed = reader.read("attendance.csv",
                new ByteArrayInputStream(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThat(parsed.importedRows()).isEqualTo(1);
        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows().get(0).deviceUserId()).isEqualTo("10");
        assertThat(parsed.rows().get(0).employeeName()).isEqualTo("Ahmed");
        assertThat(parsed.rows().get(0).punchedAt())
                .isEqualTo(LocalDate.of(2026, 7, 24).atTime(8, 0).atZone(zone).toInstant());
    }

    @Test
    void readsTheProvidedZktecoAccessBackupWhenAvailable() throws Exception {
        Path backup = Path.of("C:/Users/wolfn/Downloads/attBackup.mdb");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(backup));

        try (var input = Files.newInputStream(backup)) {
            var parsed = reader.read(backup.getFileName().toString(), input);
            assertThat(parsed.totalRows()).isEqualTo(18_719);
            assertThat(parsed.importedRows()).isEqualTo(18_719);
            assertThat(parsed.errors()).isEmpty();
            assertThat(parsed.rows()).allSatisfy(row -> {
                assertThat(row.deviceUserId()).isNotBlank();
                assertThat(row.punchedAt()).isNotNull();
            });
        }
    }
}
