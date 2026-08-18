package com.bemo.hr.workforce.application;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

/**
 * Bulk master-data import for the existing Workforce APIs.
 *
 * <p>The implementation intentionally delegates every row to the same WorkerService /
 * ContractorService create methods used by the UI.  Reflection is used only at the
 * narrow request-construction boundary so this patch remains compatible with small
 * record-signature changes on the consolidated branch while backend validation remains
 * authoritative.</p>
 *
 * <p>Named WorkforceMasterDataExcelImportService (rather than WorkforceExcelImportService)
 * so its Spring bean does not collide with the existing workforce daily-attendance Excel
 * import service in com.bemo.hr.workforce.</p>
 */
@Service
@Slf4j
public class WorkforceMasterDataExcelImportService {
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final int MAX_ERRORS = 200;

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final DataFormatter formatter = new DataFormatter(Locale.ROOT);

    public WorkforceMasterDataExcelImportService(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    private static void required(String value, String field) {
        if (isBlank(value)) throw new IllegalArgumentException(field + " is required.");
    }

    private static String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.strip();
    }

    private static String nullIfBlank(String value) {
        return isBlank(value) ? null : value.strip();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

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

    public byte[] templateWorkbook(String kind) {
        String normalizedKind = kind == null ? "" : kind.strip().toLowerCase(Locale.ROOT);
        if (!normalizedKind.equals("workers") && !normalizedKind.equals("contractors")) {
            throw new IllegalArgumentException("kind must be workers or contractors.");
        }
        String[] headers = normalizedKind.equals("workers")
                ? new String[]{"WorkerCode", "FullName", "NationalID", "ContractorCode", "Category", "DefaultDailyRate", "StandardDailyHours", "BranchId", "AttendanceMode", "Phone", "Status", "Notes"}
                : new String[]{"ContractorCode", "ContractorName", "TradeName", "Phone", "SecondaryPhone", "TaxId", "Address", "AccountingModel", "PaymentRouting", "SettlementCycleDays", "DefaultDailyRate", "FeeType", "FeeValue", "FeeBase", "FixedPeriodAmount", "Status", "Notes"};
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(normalizedKind.equals("workers") ? "Workers" : "Contractors");
            Row header = sheet.createRow(0);
            var headerStyle = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int index = 0; index < headers.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, Math.min(45, Math.max(14, headers[index].length() + 5)) * 256);
            }
            sheet.createFreezePane(0, 1);

            // Keep a sample row because it makes the expected formats obvious, while clearly
            // labelling values as examples so administrators can delete/replace it before upload.
            Row example = sheet.createRow(1);
            if (normalizedKind.equals("workers")) {
                String[] values = {"W-EXAMPLE-001", "Example Worker", "", "CONT-EXAMPLE", "GENERAL", "250", "8", "", "MANUAL", "01000000000", "ACTIVE", "Delete this sample row before import"};
                for (int i = 0; i < values.length; i++) example.createCell(i).setCellValue(values[i]);
            } else {
                String[] values = {"CONT-EXAMPLE", "Example Contractor", "", "01000000000", "", "", "", "worker_net_total", "contractor_full", "30", "0", "PERCENT", "0", "WORKER_NET", "0", "ACTIVE", "Delete this sample row before import"};
                for (int i = 0; i < values.length; i++) example.createCell(i).setCellValue(values[i]);
            }

            Sheet instructions = workbook.createSheet("Instructions");
            instructions.setColumnWidth(0, 34 * 256);
            instructions.setColumnWidth(1, 90 * 256);
            Row title = instructions.createRow(0);
            title.createCell(0).setCellValue("Field / Rule");
            title.createCell(1).setCellValue("Description");
            title.getCell(0).setCellStyle(headerStyle);
            title.getCell(1).setCellStyle(headerStyle);
            String[][] rows = normalizedKind.equals("workers")
                    ? new String[][]{
                    {"WorkerCode", "Required in USER_CODE mode. In AUTO mode NationalID may be used when WorkerCode is blank."},
                    {"FullName", "Optional; defaults to Worker <code> if blank."},
                    {"ContractorCode", "Required. Must match an existing contractor code."},
                    {"Category", "Required. Must match an existing employee/attendance category code."},
                    {"AttendanceMode", "Optional; defaults to MANUAL."},
                    {"Status", "Optional; defaults to ACTIVE."}
            }
                    : new String[][]{
                    {"ContractorCode", "Required in USER_CODE mode. In AUTO mode TaxId may be used when code is blank."},
                    {"ContractorName", "Required."},
                    {"AccountingModel", "Optional; defaults to worker_net_total."},
                    {"PaymentRouting", "Optional; defaults to contractor_full."},
                    {"FeeType", "Optional; defaults to PERCENT."},
                    {"Status", "Optional; defaults to ACTIVE."}
            };
            for (int i = 0; i < rows.length; i++) {
                Row row = instructions.createRow(i + 1);
                row.createCell(0).setCellValue(rows[i][0]);
                row.createCell(1).setCellValue(rows[i][1]);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate Excel template: " + rootMessage(ex), ex);
        }
    }

    private void importWorker(Map<String, Integer> headers, Row row, String mode) throws Exception {
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
            contractorId = resolveId("com.bemo.hr.workforce.infrastructure.ContractorRepository", "findByCode", contractorCode);
        }
        String categoryId = text(headers, row, "categoryid");
        if (isBlank(categoryId)) {
            String categoryCode = text(headers, row, "categorycode", "category", "workercategory", "الفئة", "فئةالعامل");
            categoryId = resolveId("com.bemo.hr.attendance.infrastructure.AttendanceCategoryRepository", "findByCodeIgnoreCase", categoryCode);
        }
        required(contractorId, "ContractorCode/ContractorId");
        required(categoryId, "Category/CategoryId");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("fullName", fullName);
        payload.put("contractorId", contractorId);
        payload.put("categoryId", categoryId);
        payload.put("defaultDailyRate", decimal(headers, row, BigDecimal.ZERO, "defaultdailyrate", "dailyrate", "اليوميةالافتراضية"));
        payload.put("standardDailyHours", decimal(headers, row, BigDecimal.valueOf(8), "standarddailyhours", "dailyhours", "ساعاتاليومالقياسية"));
        payload.put("branchId", nullIfBlank(text(headers, row, "branchid", "branch", "الفرع")));
        payload.put("attendanceMode", firstNonBlank(text(headers, row, "attendancemode", "attendance", "طريقةالحضور"), "MANUAL").toUpperCase(Locale.ROOT));
        payload.put("status", activeStatus(headers, row));
        payload.put("phone", nullIfBlank(text(headers, row, "phone", "mobile", "رقمالهاتف")));
        payload.put("nationalId", nullIfBlank(nationalId));
        payload.put("notes", nullIfBlank(text(headers, row, "notes", "ملاحظات")));
        invokeCreate("com.bemo.hr.workforce.application.WorkerService",
                "com.bemo.hr.workforce.api.WorkforceApi$WorkerRequest", payload);
    }

    private void importContractor(Map<String, Integer> headers, Row row, String mode) throws Exception {
        String code = text(headers, row, "contractorcode", "usercode", "code", "كودالمقاول", "كودالمستخدم");
        String taxId = text(headers, row, "taxid", "taxnumber", "taxregistration", "الرقمالضريبي");
        if (isBlank(code) && "AUTO".equals(mode) && !isBlank(taxId)) code = taxId;
        required(code, "ContractorCode/UserCode");
        String name = text(headers, row, "contractorname", "name", "اسمالمقاول", "الاسم");
        required(name, "ContractorName");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("name", name);
        payload.put("tradeName", nullIfBlank(text(headers, row, "tradename", "commercialname", "الاسمالتجاري")));
        payload.put("phone", firstNonBlank(text(headers, row, "phone", "mobile", "رقمالهاتف"), "-"));
        payload.put("secondaryPhone", nullIfBlank(text(headers, row, "secondaryphone", "phone2", "هاتفثانوي")));
        payload.put("taxId", nullIfBlank(taxId));
        payload.put("address", nullIfBlank(text(headers, row, "address", "العنوان")));
        payload.put("accountingModel", firstNonBlank(text(headers, row, "accountingmodel", "model", "نموذجالحساب"), "worker_net_total"));
        payload.put("paymentRouting", firstNonBlank(text(headers, row, "paymentrouting", "routing", "مسارالسداد"), "contractor_full"));
        payload.put("settlementCycleDays", integer(headers, row, 30, "settlementcycledays", "paymenttermsdays", "cycledays", "دورةالتسوية"));
        payload.put("defaultDailyRate", decimal(headers, row, BigDecimal.ZERO, "defaultdailyrate", "dailyrate", "اليوميةالافتراضية"));
        payload.put("feeType", firstNonBlank(text(headers, row, "feetype", "نوعالعمولة"), "PERCENT"));
        payload.put("feeValue", decimal(headers, row, BigDecimal.ZERO, "feevalue", "fee", "العمولة"));
        payload.put("feeBase", firstNonBlank(text(headers, row, "feebase", "أساسالعمولة"), "WORKER_NET"));
        payload.put("fixedPeriodAmount", decimal(headers, row, BigDecimal.ZERO, "fixedperiodamount", "fixedamount", "مبلغالفترة"));
        payload.put("status", activeStatus(headers, row));
        payload.put("notes", nullIfBlank(text(headers, row, "notes", "ملاحظات")));
        invokeCreate("com.bemo.hr.workforce.application.ContractorService",
                "com.bemo.hr.workforce.api.WorkforceApi$ContractorRequest", payload);
    }

    private void invokeCreate(String serviceClassName, String requestClassName, Map<String, Object> payload) throws Exception {
        Class<?> serviceClass = Class.forName(serviceClassName);
        Class<?> requestClass = Class.forName(requestClassName);
        Object service = applicationContext.getBean(serviceClass);
        Object request = objectMapper.convertValue(payload, requestClass);
        Method create = null;
        for (Method method : serviceClass.getMethods()) {
            if (method.getName().equals("create") && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(requestClass)) {
                create = method;
                break;
            }
        }
        if (create == null) {
            for (Method method : serviceClass.getMethods()) {
                if (method.getName().equals("create") && method.getParameterCount() == 1) {
                    create = method;
                    break;
                }
            }
        }
        if (create == null) throw new IllegalStateException("No create(request) method found on " + serviceClassName);
        try {
            create.invoke(service, request);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getTargetException();
            if (cause instanceof Exception exception) throw exception;
            throw ex;
        }
    }

    private String resolveId(String repositoryClassName, String finderName, String value) throws Exception {
        if (isBlank(value)) return null;
        Class<?> repositoryClass = Class.forName(repositoryClassName);
        Object repository = applicationContext.getBean(repositoryClass);
        Method finder = repositoryClass.getMethod(finderName, String.class);
        Object result = finder.invoke(repository, value.strip());
        Object entity = result instanceof Optional<?> optional ? optional.orElse(null) : result;
        if (entity == null) return null;
        Method getId = entity.getClass().getMethod("getId");
        Object id = getId.invoke(entity);
        return id == null ? null : String.valueOf(id);
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

    private int integer(Map<String, Integer> headers, Row row, int fallback, String... names) {
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

    public record RowError(int rowNumber, String message) {
    }

    public record ImportResult(int totalRows, int importedRows, int failedRows, List<RowError> errors,
                               String identityMode) {
    }
}
