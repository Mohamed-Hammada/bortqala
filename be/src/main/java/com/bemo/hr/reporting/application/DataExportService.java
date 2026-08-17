package com.bemo.hr.reporting.application;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.reporting.infrastructure.ExcelExportSupport;
import com.bemo.hr.shared.i18n.TranslationService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DataExportService {
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final TranslationService translationService;
    private final BusinessPartyRepository businessPartyRepository;
    private final DashboardService dashboardService;

    public byte[] categories(ExcelExportOptions options) {
        var messages = ExcelExportSupport.messages(translationService, options);
        var rows = attendanceCategoryRepository.findByScopeInOrderByNameAsc(java.util.List.of(
                com.bemo.hr.employee.domain.CategoryScope.EMPLOYEE,
                com.bemo.hr.employee.domain.CategoryScope.BOTH)).stream().<List<?>>map(item -> List.of(
                item.getCode(), item.getName(), item.getExpectedDailyMinutes(),
                ExcelExportSupport.enumText(messages, item.getAttendanceMode()),
                ExcelExportSupport.enumText(messages, item.getPayCycle()),
                ExcelExportSupport.text(messages, item.isActive() ? "export.value.yes" : "export.value.no"))).toList();
        return workbook("export.sheet.categories", "CategoriesTable", List.of("code", "name", "defaultMinutes",
                "attendanceMode", "payCycle", "active"), rows, options);
    }

    public byte[] employees(ExcelExportOptions options) {
        var messages = ExcelExportSupport.messages(translationService, options);
        var categories = attendanceCategoryRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(item -> item.getId(), item -> item.getName()));
        var rows = employeeRepository.findAllByOrderByFullNameAsc().stream().<List<?>>map(item -> List.of(
                item.getEmployeeCode(), item.getFullName(), item.getDeviceUserId() == null ? "" : item.getDeviceUserId(),
                categories.getOrDefault(item.getCategoryId(), ""),
                ExcelExportSupport.enumText(messages, item.getEmploymentType()), item.getActiveFrom(),
                item.getActiveTo() == null ? "" : item.getActiveTo(),
                ExcelExportSupport.text(messages, item.isActive() ? "export.value.yes" : "export.value.no"))).toList();
        return workbook("export.sheet.employees", "EmployeesTable", List.of("code", "name", "deviceUserId",
                "category", "employmentType", "from", "to", "active"), rows, options);
    }

    public byte[] imports(ExcelExportOptions options) {
        var messages = ExcelExportSupport.messages(translationService, options);
        var rows = importBatchRepository.findAllByOrderByImportedAtDesc().stream().<List<?>>map(item -> List.of(
                item.getFileName(), item.getDeviceName(), ExcelExportSupport.enumText(messages, item.getStatus()),
                item.getTotalRows(), item.getImportedRows(), item.getErrorRows(), item.getImportedBy(), item.getImportedAt())).toList();
        return workbook("export.sheet.imports", "ImportsTable", List.of("file", "device", "status", "rows",
                "imported", "errors", "by", "at"), rows, options);
    }

    public byte[] unmatched(ExcelExportOptions options) {
        var rows = punchRecordRepository.summarizeUnmatched().stream()
                .filter(item -> employeeRepository.findByDeviceUserId((String) item[0]).isEmpty())
                .<List<?>>map(item -> List.of(String.valueOf(item[0]), item[1] == null ? "" : String.valueOf(item[1]),
                        ((Number) item[2]).longValue(), item[3], item[4])).toList();
        return workbook("export.sheet.unmatched", "UnmatchedTable", List.of("deviceUserId", "observedName",
                "punchCount", "firstPunch", "lastPunch"), rows, options);
    }

    public byte[] parties(ExcelExportOptions options) {
        var messages = ExcelExportSupport.messages(translationService, options);
        var rows = businessPartyRepository.findAllByOrderByNameAsc().stream().<List<?>>map(item -> List.of(
                item.getCode(), item.getName(), partyType(messages, item.getPartyType()),
                item.getContactPerson() == null ? "" : item.getContactPerson(),
                item.getPhone() == null ? "" : item.getPhone(), item.getNotes() == null ? "" : item.getNotes(),
                ExcelExportSupport.text(messages, item.isActive() ? "export.value.yes" : "export.value.no"))).toList();
        return workbook("export.sheet.parties", "BusinessPartiesTable", List.of("code", "name", "type",
                "contactPerson", "phone", "notes", "active"), rows, options);
    }

    public byte[] trends(int months, ExcelExportOptions options) {
        var current = java.time.YearMonth.now();
        return trends(months, current.getYear(), current.getMonthValue(), options);
    }

    public byte[] trends(int months, int year, int month, ExcelExportOptions options) {
        var messages = ExcelExportSupport.messages(translationService, options);
        var rows = dashboardService.trends(months, year, month).stream().<List<?>>map(point -> List.of(
                point.label(), point.scheduledEmployeeDays(), point.presentEmployeeDays(),
                point.attendanceRate() + "%", point.exceptionDays(), point.overtimeMinutes(),
                point.paidCount(), point.pendingCount(), point.totalGross(), point.totalPaid())).toList();
        return workbook("export.sheet.trends", "MultiPeriodTrendsTable", List.of("month", "scheduledDays",
                "presentDays", "attendanceRate", "exceptionDays", "overtimeMinutes", "paidCount",
                "pendingCount", "grossTotal", "paidTotal"), rows, options);
    }

    private byte[] workbook(String sheetKey, String tableName, List<String> headerKeys,
                            List<? extends List<?>> rows, ExcelExportOptions options) {
        var messages = ExcelExportSupport.messages(translationService, options);
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = ExcelExportSupport.sheet(workbook, ExcelExportSupport.text(messages, sheetKey), options.rightToLeft());
            var headers = headerKeys.stream().map(key -> ExcelExportSupport.text(messages, "export.column." + key)).toList();
            ExcelExportSupport.writeHeader(sheet, headers);
            var styles = ExcelExportSupport.styles(workbook);
            int rowIndex = 1;
            for (var values : rows) ExcelExportSupport.writeRow(sheet, rowIndex++, values, styles);
            ExcelExportSupport.finishTable(sheet, rowIndex - 1, headers.size(), tableName, options);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create Excel workbook.", exception);
        }
    }

    private String partyType(java.util.Map<String, String> messages, String type) {
        String key = switch (type) {
            case "SUPPLIER" -> "partyType.supplier";
            case "PROCESSING_CUSTOMER" -> "partyType.processingCustomer";
            case "EXPORT_CUSTOMER" -> "partyType.exportCustomer";
            case "FARM" -> "partyType.farm";
            default -> null;
        };
        return key == null ? type.replace('_', ' ') : ExcelExportSupport.text(messages, key);
    }
}
