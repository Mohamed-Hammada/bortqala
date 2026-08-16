package com.bemo.hr.workforce.application;

import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.workforce.ContractorRepository;
import com.bemo.hr.workforce.ContractorService;
import com.bemo.hr.workforce.WorkerService;
import com.bemo.hr.workforce.WorkforceApi;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Bulk master-data import for the Workforce APIs (Workers & Contractors).
 */
@Service("workforceMasterDataExcelImportService")
public class WorkforceExcelImportService {
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final int MAX_ERRORS = 200;

    private final WorkerService workerService;
    private final ContractorService contractorService;
    private final ContractorRepository contractorRepository;
    private final AttendanceCategoryRepository categoryRepository;
    private final DataFormatter formatter = new DataFormatter(Locale.ROOT);

    public WorkforceExcelImportService(
            WorkerService workerService,
            ContractorService contractorService,
            ContractorRepository contractorRepository,
            AttendanceCategoryRepository categoryRepository) {
        this.workerService = workerService;
        this.contractorService = contractorService;
        this.contractorRepository = contractorRepository;
        this.categoryRepository = categoryRepository;
    }

    public ImportResult importWorkbook(String kind, MultipartFile file, String identityMode) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Excel file exceeds the 20 MB limit.");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!filename.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$")) {
            throw new IllegalArgumentException("Only .xlsx and .xls files are supported.");
        }

        String normalizedKind = kind == null ? "" : kind.strip().toLowerCase(Locale.ROOT);
        if (!normalizedKind.equals("workers") && !normalizedKind.equals("contractors")) {
            throw new IllegalArgumentException("kind must be workers or contractors.");
        }
        String mode = "AUTO".equalsIgnoreCase(identityMode) ? "AUTO" : "USER_CODE";

        List<RowError> errors = new ArrayList<>();
        int total = 0;
        int imported = 0;
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("The workbook does not contain data rows.");
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> headers = headers(headerRow);
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || blank(row)) continue;
                total++;
                try {
                    if (normalizedKind.equals("workers")) {
                        importWorker(headers, row, mode);
                    } else {
                        importContractor(headers, row, mode);
                    }
                    imported++;
                } catch (Exception ex) {
                    if (errors.size() < MAX_ERRORS) {
                        errors.add(new RowError(rowIndex + 1, rootMessage(ex)));
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read the Excel workbook: " + rootMessage(ex), ex);
        }
        return new ImportResult(total, imported, Math.max(0, total - imported), List.copyOf(errors), mode);
    }

    private void importWorker(Map<String, Integer> headers, Row row, String mode) {
        String code = text(headers, row, "workercode", "employeecode", "usercode", "code", "كودالعامل", "كودالمستخدم");
        String nationalId = text(headers, row, "nationalid", "nationalnumber", "الرقمالقومي");
        if (isBlank(code) && "AUTO".equals(mode) && !isBlank(nationalId)) code = nationalId;
        required(code, "WorkerCode/UserCode");
        String fullName = firstNonBlank(
                text(headers, row, "fullname", "workername", "name", "الاسمالكامل", "اسمالعامل"),
                "Worker " + code);

        String contractorId = text(headers, row, "contractorid");
        if (isBlank(contractorId)) {
            String contractorCode = text(headers, row, "contractorcode", "contractor", "كودالمقاول", "المقاول");
            if (!isBlank(contractorCode)) {
                contractorId = contractorRepository.findByCode(contractorCode.strip())
                        .map(c -> c.getId())
                        .orElse(null);
            }
        }
        String categoryId = text(headers, row, "categoryid");
        if (isBlank(categoryId)) {
            String categoryCode = text(headers, row, "categorycode", "category", "workercategory", "الفئة", "فئةالعامل");
            if (!isBlank(categoryCode)) {
                categoryId = categoryRepository.findByCodeIgnoreCase(categoryCode.strip())
                        .map(c -> c.getId())
                        .orElse(null);
            }
        }
        required(contractorId, "ContractorCode/ContractorId");
        required(categoryId, "Category/CategoryId");

        WorkforceApi.WorkerRequest request = new WorkforceApi.WorkerRequest(
                code,
                fullName,
                contractorId,
                categoryId,
                decimal(headers, row, BigDecimal.ZERO, "defaultdailyrate", "dailyrate", "اليوميةالافتراضية"),
                decimal(headers, row, BigDecimal.valueOf(8), "standarddailyhours", "dailyhours", "ساعاتاليومالقياسية"),
                nullIfBlank(text(headers, row, "branchid", "branch", "الفرع")),
                firstNonBlank(text(headers, row, "attendancemode", "attendance", "طريقةالحضور"), "MANUAL").toUpperCase(Locale.ROOT),
                activeStatus(headers, row),
                nullIfBlank(text(headers, row, "phone", "mobile", "رقمالهاتف")),
                nullIfBlank(nationalId),
                nullIfBlank(text(headers, row, "notes", "ملاحظات"))
        );
        workerService.create(request);
    }

    private void importContractor(Map<String, Integer> headers, Row row, String mode) {
        String code = text(headers, row, "contractorcode", "usercode", "code", "كودالمقاول", "كودالمستخدم");
        String taxId = text(headers, row, "taxid", "taxnumber", "taxregistration", "الرقمالضريبي");
        if (isBlank(code) && "AUTO".equals(mode) && !isBlank(taxId)) code = taxId;
        required(code, "ContractorCode/UserCode");
        String name = text(headers, row, "contractorname", "name", "اسمالمقاول", "الاسم");
        required(name, "ContractorName");

        WorkforceApi.ContractorRequest request = new WorkforceApi.ContractorRequest(
                code,
                name,
                nullIfBlank(text(headers, row, "tradename", "commercialname", "الاسمالتجاري")),
                firstNonBlank(text(headers, row, "phone", "mobile", "رقمالهاتف"), "-"),
                nullIfBlank(text(headers, row, "secondaryphone", "phone2", "هاتفثانوي")),
                nullIfBlank(taxId),
                nullIfBlank(text(headers, row, "address", "العنوان")),
                firstNonBlank(text(headers, row, "accountingmodel", "model", "نموذجالحساب"), "worker_net_total"),
                firstNonBlank(text(headers, row, "paymentrouting", "routing", "مسارالسداد"), "contractor_full"),
                integer(headers, row, 30, "settlementcycledays", "paymenttermsdays", "cycledays", "دورةالتسوية"),
                decimal(headers, row, BigDecimal.ZERO, "defaultdailyrate", "dailyrate", "اليوميةالافتراضية"),
                firstNonBlank(text(headers, row, "feetype", "نوعالعمولة"), "PERCENT"),
                decimal(headers, row, BigDecimal.ZERO, "feevalue", "fee", "العمولة"),
                firstNonBlank(text(headers, row, "feebase", "أساسالعمولة"), "WORKER_NET"),
                decimal(headers, row, BigDecimal.ZERO, "fixedperiodamount", "fixedamount", "مبلغالفترة"),
                activeStatus(headers, row),
                nullIfBlank(text(headers, row, "notes", "ملاحظات"))
        );
        contractorService.create(request);
    }

    private Map<String, Integer> headers(Row row) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (row == null) return result;
        for (Cell cell : row) {
            String key = normalize(formatter.formatCellValue(cell));
            if (!key.isBlank()) result.putIfAbsent(key, cell.getColumnIndex());
        }
        return result;
    }

    private String text(Map<String, Integer> headers, Row row, String... names) {
        for (String name : names) {
            Integer index = headers.get(normalize(name));
            if (index == null) continue;
            Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) continue;
            String value = formatter.formatCellValue(cell).strip();
            if (!value.isBlank()) return value;
        }
        return null;
    }

    private BigDecimal decimal(Map<String, Integer> headers, Row row, BigDecimal fallback, String... names) {
        String value = text(headers, row, names);
        if (isBlank(value)) return fallback;
        try {
            return new BigDecimal(value.replace(",", "").strip());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(names[0] + " must be numeric.");
        }
    }

    private Integer integer(Map<String, Integer> headers, Row row, Integer fallback, String... names) {
        String value = text(headers, row, names);
        if (isBlank(value)) return fallback;
        try {
            return new BigDecimal(value.replace(",", "").strip()).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException(names[0] + " must be an integer.");
        }
    }

    private String activeStatus(Map<String, Integer> headers, Row row) {
        String status = text(headers, row, "status", "الحالة");
        if (!isBlank(status)) return status.toUpperCase(Locale.ROOT);
        String active = text(headers, row, "active", "نشط");
        if (isBlank(active)) return "ACTIVE";
        String normalized = active.strip().toLowerCase(Locale.ROOT);
        return List.of("false", "0", "no", "n", "inactive", "غيرنشط").contains(normalize(normalized))
                ? "INACTIVE" : "ACTIVE";
    }

    private boolean blank(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !formatter.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private static void required(String value, String field) {
        if (isBlank(value)) throw new IllegalArgumentException(field + " is required.");
    }

    private static String firstNonBlank(String value, String fallback) { return isBlank(value) ? fallback : value.strip(); }
    private static String nullIfBlank(String value) { return isBlank(value) ? null : value.strip(); }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static String normalize(String value) {
        if (value == null) return "";
        return value.strip().toLowerCase(Locale.ROOT)
                .replace("_", "").replace("-", "").replace(" ", "")
                .replace("/", "").replace("\\", "").replace(".", "");
    }
    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    public record RowError(int rowNumber, String message) { }
    public record ImportResult(int totalRows, int importedRows, int failedRows, List<RowError> errors, String identityMode) { }
}
