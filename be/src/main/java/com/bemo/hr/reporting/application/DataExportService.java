package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DataExportService {
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;

    public DataExportService(AttendanceCategoryRepository attendanceCategoryRepository, EmployeeRepository employeeRepository,
                             ImportBatchRepository importBatchRepository, PunchRecordRepository punchRecordRepository) {
        this.attendanceCategoryRepository = attendanceCategoryRepository; this.employeeRepository = employeeRepository;
        this.importBatchRepository = importBatchRepository; this.punchRecordRepository = punchRecordRepository;
    }

    public byte[] categories() {
        var rows = attendanceCategoryRepository.findAllByOrderByNameAsc().stream().map(item -> List.of(item.getCode(), item.getName(),
                String.valueOf(item.getExpectedDailyMinutes()), item.getAttendanceMode().name(), item.getPayCycle().name(), item.isActive() ? "Yes" : "No")).toList();
        return workbook("Categories", List.of("Code", "Name", "Default minutes", "Attendance mode", "Pay cycle", "Active"), rows);
    }
    public byte[] employees() {
        var categories = attendanceCategoryRepository.findAll().stream().collect(java.util.stream.Collectors.toMap(item -> item.getId(), item -> item.getName()));
        var rows = employeeRepository.findAllByOrderByFullNameAsc().stream().map(item -> List.of(item.getEmployeeCode(), item.getFullName(),
                item.getDeviceUserId() == null ? "" : item.getDeviceUserId(), categories.getOrDefault(item.getCategoryId(), ""), item.getEmploymentType().name(),
                item.getActiveFrom().toString(), item.getActiveTo() == null ? "" : item.getActiveTo().toString(), item.isActive() ? "Yes" : "No")).toList();
        return workbook("Employees", List.of("Code", "Name", "Device user id", "Category", "Employment type", "From", "To", "Active"), rows);
    }
    public byte[] imports() {
        var rows = importBatchRepository.findAllByOrderByImportedAtDesc().stream().map(item -> List.of(item.getFileName(), item.getDeviceName(), item.getStatus().name(),
                String.valueOf(item.getTotalRows()), String.valueOf(item.getImportedRows()), String.valueOf(item.getErrorRows()), item.getImportedBy(), item.getImportedAt().toString())).toList();
        return workbook("Imports", List.of("File", "Device", "Status", "Rows", "Imported", "Errors", "By", "At"), rows);
    }
    public byte[] unmatched() {
        var rows = punchRecordRepository.summarizeUnmatched().stream().filter(item -> employeeRepository.findByDeviceUserId((String) item[0]).isEmpty())
                .map(item -> List.of(String.valueOf(item[0]), item[1] == null ? "" : String.valueOf(item[1]), String.valueOf(item[2]), String.valueOf(item[3]), String.valueOf(item[4]))).toList();
        return workbook("Unmatched", List.of("Device user id", "Observed name", "Punch count", "First punch", "Last punch"), rows);
    }

    private byte[] workbook(String sheetName, List<String> headers, List<? extends List<String>> rows) {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sheetName); sheet.setRightToLeft(true); sheet.createFreezePane(0, 1);
            var style = workbook.createCellStyle(); style.setFillForegroundColor(IndexedColors.GOLD.getIndex()); style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var font = workbook.createFont(); font.setBold(true); style.setFont(font);
            var header = sheet.createRow(0); for (int col = 0; col < headers.size(); col++) { var cell = header.createCell(col); cell.setCellValue(headers.get(col)); cell.setCellStyle(style); }
            int rowIndex = 1; for (var values : rows) { var row = sheet.createRow(rowIndex++); for (int col = 0; col < values.size(); col++) row.createCell(col).setCellValue(values.get(col)); }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, headers.size() - 1));
            for (int col = 0; col < headers.size(); col++) { sheet.autoSizeColumn(col); sheet.setColumnWidth(col, Math.min(sheet.getColumnWidth(col) + 512, 12_000)); }
            workbook.write(output); return output.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException("Could not create Excel workbook.", exception); }
    }
}
