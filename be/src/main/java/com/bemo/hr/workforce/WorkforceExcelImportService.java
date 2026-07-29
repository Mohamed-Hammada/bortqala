package com.bemo.hr.workforce;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkforceExcelImportService {
    private static final List<String> REQUIRED_FIELDS = List.of("workerCode", "workDate", "attendanceValue");

    private final WorkforceImportBatchRepository batchRepository;
    private final WorkforceImportRowRepository rowRepository;
    private final WorkforceImportChangeRepository changeRepository;
    private final WorkerRepository workerRepository;
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final AuditService auditService;

    public record ImportBatchResponse(String id, String fileName, String checksum, String status,
                                      List<String> headers, Map<String, String> columnMapping,
                                      int totalRows, int validRows, int invalidRows, int importedRows,
                                      String createdBy, long createdAt, Long importedAt, Long reversedAt) { }
    public record MappingRequest(Map<String, String> columns) { }
    public record CommitRequest(String operationId, boolean importValidRowsOnly) { }
    public record ImportRowResponse(int rowNumber, String workerCode, String workerName, String workDate,
                                    BigDecimal attendanceValue, String validationStatus,
                                    String errorCode, String errorMessage) { }
    public record ValidationResponse(ImportBatchResponse batch, List<ImportRowResponse> preview,
                                     int warningCount, boolean canCommitAll, boolean canCommitValidRows) { }
    public record CommitResponse(ImportBatchResponse batch, int createdRows, int updatedRows,
                                 int skippedInvalidRows, boolean idempotentReplay) { }
    public record ImportDiagnosticResult(int totalSheetsProcessed, int totalRowsParsed,
                                         BigDecimal totalDaysInSummary, BigDecimal totalDaysInSettlement,
                                         BigDecimal discrepancyDays, boolean requiresReconciliationWarning,
                                         List<String> warnings) { }

    @Transactional(readOnly = true)
    public List<ImportBatchResponse> listBatches() {
        return batchRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapBatch).toList();
    }

    public ImportBatchResponse getBatch(String batchId) {
        return mapBatch(requireBatch(batchId));
    }

    @Transactional
    public ImportBatchResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessRuleException("اختر ملف Excel غير فارغ.");
        String fileName = file.getOriginalFilename() == null ? "workforce-import.xlsx" : file.getOriginalFilename();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessRuleException("صيغة الملف المدعومة هي XLSX فقط.");
        }
        try {
            byte[] bytes = file.getBytes();
            String checksum = sha256(bytes);
            batchRepository.findByChecksum(checksum).ifPresent(existing -> {
                throw new BusinessRuleException("تم رفع هذا الملف من قبل ضمن العملية " + existing.getId()
                        + " وحالتها " + existing.getStatus() + ".");
            });
            List<String> headers = readHeaders(bytes);
            WorkforceImportBatch batch = batchRepository.save(new WorkforceImportBatch(fileName,
                    file.getContentType(), checksum, bytes, String.join("\t", headers), actor()));
            auditService.record("UPLOAD", "WORKFORCE_IMPORT", batch.getId(), actor(),
                    "{\"fileName\":\"" + json(fileName) + "\",\"checksum\":\"" + checksum + "\"}", null);
            return mapBatch(batch);
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException("تعذر قراءة ملف Excel: " + exception.getMessage());
        }
    }

    @Transactional
    public ImportBatchResponse saveMapping(String batchId, MappingRequest request) {
        WorkforceImportBatch batch = requireEditableBatch(batchId);
        Map<String, String> columns = request == null || request.columns() == null ? Map.of() : request.columns();
        List<String> headers = headers(batch);
        for (String field : REQUIRED_FIELDS) {
            String header = columns.get(field);
            if (header == null || !headers.contains(header)) {
                throw new BusinessRuleException("يجب ربط الحقل " + field + " بعمود موجود في الملف.");
            }
        }
        batch.map(encodeMapping(columns));
        auditService.record("MAP_COLUMNS", "WORKFORCE_IMPORT", batchId, actor(),
                "{\"columns\":\"" + json(batch.getColumnMapping()) + "\"}", null);
        return mapBatch(batchRepository.save(batch));
    }

    @Transactional
    public ValidationResponse validate(String batchId) {
        WorkforceImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessRuleException("عملية الاستيراد غير موجودة."));
        if (!"MAPPED".equals(batch.getStatus())) {
            throw new BusinessRuleException("يجب حفظ مطابقة الأعمدة قبل التحقق، ولا يمكن إعادة كتابة نتيجة تحقق محفوظة.");
        }
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(batch.getOriginalFile()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> indexes = headerIndexes(sheet.getRow(sheet.getFirstRowNum()));
            Map<String, String> mapping = decodeMapping(batch.getColumnMapping());
            Map<String, Worker> workers = new HashMap<>();
            workerRepository.findAll().forEach(worker -> workers.put(worker.getCode().strip().toUpperCase(Locale.ROOT), worker));
            List<WorkforceImportRow> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("ar-EG"));
            int valid = 0;
            int invalid = 0;
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row sourceRow = sheet.getRow(index);
                if (sourceRow == null || isBlank(sourceRow, formatter)) continue;
                String workerCode = text(sourceRow, indexes.get(mapping.get("workerCode")), formatter).strip();
                Worker worker = workers.get(workerCode.toUpperCase(Locale.ROOT));
                String workDate = parseDate(sourceRow.getCell(indexes.get(mapping.get("workDate"))), formatter);
                BigDecimal attendance = parseAttendance(text(sourceRow, indexes.get(mapping.get("attendanceValue")), formatter));
                String errorCode = null;
                String errorMessage = null;
                if (workerCode.isBlank()) { errorCode = "WORKER_CODE_REQUIRED"; errorMessage = "كود العامل مطلوب."; }
                else if (worker == null) { errorCode = "WORKER_NOT_FOUND"; errorMessage = "لم يتم العثور على عامل بالكود " + workerCode; }
                else if (workDate == null) { errorCode = "INVALID_DATE"; errorMessage = "التاريخ غير صالح؛ استخدم تاريخ Excel أو yyyy-MM-dd."; }
                else if (attendance == null || attendance.signum() < 0 || attendance.compareTo(BigDecimal.ONE) > 0) {
                    errorCode = "INVALID_ATTENDANCE"; errorMessage = "قيمة الحضور يجب أن تكون 0 أو 0.5 أو 1.";
                }
                boolean ok = errorCode == null;
                if (ok) valid++; else invalid++;
                rows.add(new WorkforceImportRow(batchId, index + 1, raw(sourceRow, formatter), workerCode,
                        worker == null ? null : worker.getId(), workDate, attendance, ok ? "VALID" : "INVALID",
                        errorCode, errorMessage));
            }
            rowRepository.saveAll(rows);
            batch.validated(rows.size(), valid, invalid);
            batchRepository.save(batch);
            auditService.record("VALIDATE", "WORKFORCE_IMPORT", batchId, actor(),
                    "{\"total\":" + rows.size() + ",\"valid\":" + valid + ",\"invalid\":" + invalid + "}", null);
            return validationResponse(batch, rows);
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException("تعذر التحقق من الملف: " + exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ValidationResponse preview(String batchId) {
        WorkforceImportBatch batch = requireBatch(batchId);
        return validationResponse(batch, rowRepository.findByBatchIdOrderByRowNumberAsc(batchId));
    }

    @Transactional
    public CommitResponse commit(String batchId, CommitRequest request) {
        WorkforceImportBatch batch = requireBatch(batchId);
        if (request == null || request.operationId() == null || request.operationId().isBlank()) {
            throw new BusinessRuleException("معرّف العملية مطلوب لمنع تكرار الاستيراد.");
        }
        if ("IMPORTED".equals(batch.getStatus())) {
            if (request.operationId().equals(batch.getOperationId())) {
                return new CommitResponse(mapBatch(batch), 0, 0, batch.getInvalidRows(), true);
            }
            throw new BusinessRuleException("تم تنفيذ هذا الاستيراد بالفعل بمعرّف عملية مختلف.");
        }
        if (!List.of("READY", "VALIDATED").contains(batch.getStatus())) throw new BusinessRuleException("الملف غير جاهز للتنفيذ.");
        if (batch.getInvalidRows() > 0 && !request.importValidRowsOnly()) {
            throw new BusinessRuleException("يوجد " + batch.getInvalidRows() + " صف غير صالح. صحح الملف أو اختر استيراد الصفوف الصحيحة فقط.");
        }
        List<WorkforceImportRow> validRows = rowRepository.findByBatchIdAndValidationStatusOrderByRowNumberAsc(batchId, "VALID");
        int created = 0;
        int updated = 0;
        for (WorkforceImportRow row : validRows) {
            var existing = attendanceRepository.findByWorkerIdAndWorkDate(row.getWorkerId(), row.getWorkDate());
            ManualAttendanceEntry entry;
            boolean createdNew = existing.isEmpty();
            BigDecimal beforeValue = null;
            String beforeSource = null;
            String beforeNotes = null;
            if (existing.isPresent()) {
                entry = existing.get(); beforeValue = entry.getAttendanceValue(); beforeSource = entry.getSource(); beforeNotes = entry.getNotes(); updated++;
                entry.update(entry.getWorkerId(), entry.getWorkDate(), row.getAttendanceValue(), entry.getCheckIn(), entry.getCheckOut(),
                        entry.getActualHours(), entry.getOvertimeHours(), entry.getDeductionHours(), entry.getEffectiveDailyRate(),
                        "EXCEL_IMPORT", "استيراد " + batch.getFileName() + " — صف " + row.getRowNumber());
            } else {
                entry = new ManualAttendanceEntry(row.getWorkerId(), row.getWorkDate(), row.getAttendanceValue(), null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "EXCEL_IMPORT",
                        "استيراد " + batch.getFileName() + " — صف " + row.getRowNumber());
                created++;
            }
            ManualAttendanceEntry saved = attendanceRepository.save(entry);
            changeRepository.save(new WorkforceImportChange(batchId, saved.getId(), createdNew,
                    beforeValue, beforeSource, beforeNotes, row.getAttendanceValue()));
        }
        batch.imported(request.operationId(), validRows.size());
        batchRepository.save(batch);
        auditService.record("COMMIT", "WORKFORCE_IMPORT", batchId, actor(),
                "{\"operationId\":\"" + json(request.operationId()) + "\",\"created\":" + created
                        + ",\"updated\":" + updated + ",\"skipped\":" + batch.getInvalidRows() + "}", null);
        return new CommitResponse(mapBatch(batch), created, updated, batch.getInvalidRows(), false);
    }

    @Transactional
    public ImportBatchResponse reverse(String batchId) {
        WorkforceImportBatch batch = requireBatch(batchId);
        if ("REVERSED".equals(batch.getStatus())) return mapBatch(batch);
        if (!"IMPORTED".equals(batch.getStatus())) throw new BusinessRuleException("يمكن التراجع عن عملية منفذة فقط.");
        for (WorkforceImportChange change : changeRepository.findByBatchIdOrderByCreatedAtDesc(batchId)) {
            ManualAttendanceEntry entry = attendanceRepository.findById(change.getAttendanceEntryId()).orElse(null);
            if (entry == null || change.getReversedAt() != null) continue;
            if (change.isCreatedNew()) {
                entry.update(entry.getWorkerId(), entry.getWorkDate(), BigDecimal.ZERO, entry.getCheckIn(), entry.getCheckOut(),
                        entry.getActualHours(), entry.getOvertimeHours(), entry.getDeductionHours(), entry.getEffectiveDailyRate(),
                        "IMPORT_REVERSAL", "قيد عكسي لعملية الاستيراد " + batchId);
            } else {
                entry.update(entry.getWorkerId(), entry.getWorkDate(), change.getBeforeValue(), entry.getCheckIn(), entry.getCheckOut(),
                        entry.getActualHours(), entry.getOvertimeHours(), entry.getDeductionHours(), entry.getEffectiveDailyRate(),
                        change.getBeforeSource(), change.getBeforeNotes());
            }
            change.reversed();
        }
        batch.reversed(actor());
        auditService.record("REVERSE", "WORKFORCE_IMPORT", batchId, actor(),
                "{\"changes\":" + changeRepository.findByBatchIdOrderByCreatedAtDesc(batchId).size() + "}", null);
        return mapBatch(batchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public byte[] originalFile(String batchId) { return requireBatch(batchId).getOriginalFile(); }

    @Transactional(readOnly = true)
    public byte[] errorWorkbook(String batchId) {
        WorkforceImportBatch batch = requireBatch(batchId);
        List<WorkforceImportRow> rows = rowRepository.findByBatchIdAndValidationStatusOrderByRowNumberAsc(batchId, "INVALID");
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("أخطاء الاستيراد"); sheet.setRightToLeft(true); sheet.createFreezePane(0, 1);
            var headerStyle = workbook.createCellStyle();
            var font = workbook.createFont(); font.setBold(true); font.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font); headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            String[] headers = {"رقم الصف", "كود العامل", "التاريخ", "قيمة الحضور", "كود الخطأ", "سبب الخطأ", "البيانات الأصلية"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) { Cell cell = header.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(headerStyle); }
            int outputRow = 1;
            for (WorkforceImportRow item : rows) {
                Row row = sheet.createRow(outputRow++); row.createCell(0).setCellValue(item.getRowNumber());
                row.createCell(1).setCellValue(item.getWorkerCode()); row.createCell(2).setCellValue(item.getWorkDate());
                if (item.getAttendanceValue() != null) row.createCell(3).setCellValue(item.getAttendanceValue().doubleValue());
                row.createCell(4).setCellValue(item.getErrorCode()); row.createCell(5).setCellValue(item.getErrorMessage());
                row.createCell(6).setCellValue(item.getRawData());
            }
            for (int i = 0; i < headers.length; i++) { sheet.autoSizeColumn(i); sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i), 12000)); }
            if (!rows.isEmpty()) {
                var table = sheet.createTable(new org.apache.poi.ss.util.AreaReference("A1:G" + (rows.size() + 1),
                        org.apache.poi.ss.SpreadsheetVersion.EXCEL2007));
                table.setName("WorkforceImportErrors"); table.setDisplayName("WorkforceImportErrors");
                table.getCTTable().addNewTableStyleInfo().setName("TableStyleMedium2");
            }
            workbook.write(output); return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessRuleException("تعذر إنشاء ملف أخطاء الاستيراد.");
        }
    }

    public ImportDiagnosticResult analyzeExcelImport(BigDecimal summaryDays, BigDecimal settlementDays) {
        BigDecimal summary = summaryDays == null ? BigDecimal.ZERO : summaryDays;
        BigDecimal settlement = settlementDays == null ? BigDecimal.ZERO : settlementDays;
        BigDecimal diff = settlement.subtract(summary).abs();
        return new ImportDiagnosticResult(0, 0, summary, settlement, diff, diff.signum() > 0,
                diff.signum() > 0 ? List.of("يوجد فرق ويجب استخدام دورة الاستيراد الفعلية لمراجعته.") : List.of());
    }

    private ValidationResponse validationResponse(WorkforceImportBatch batch, List<WorkforceImportRow> rows) {
        Map<String, String> workerNames = new HashMap<>();
        workerRepository.findAll().forEach(worker -> workerNames.put(worker.getId(), worker.getFullName()));
        List<ImportRowResponse> preview = rows.stream().limit(100).map(row -> new ImportRowResponse(row.getRowNumber(),
                row.getWorkerCode(), workerNames.get(row.getWorkerId()), row.getWorkDate(), row.getAttendanceValue(),
                row.getValidationStatus(), row.getErrorCode(), row.getErrorMessage())).toList();
        return new ValidationResponse(mapBatch(batch), preview, batch.getInvalidRows(), batch.getInvalidRows() == 0,
                batch.getValidRows() > 0);
    }

    private WorkforceImportBatch requireBatch(String id) {
        return batchRepository.findById(id).orElseThrow(() -> new BusinessRuleException("عملية الاستيراد غير موجودة."));
    }
    private WorkforceImportBatch requireEditableBatch(String id) {
        WorkforceImportBatch batch = requireBatch(id);
        if (!List.of("UPLOADED", "MAPPED").contains(batch.getStatus())) throw new BusinessRuleException("لا يمكن تعديل المطابقة بعد التحقق أو التنفيذ.");
        return batch;
    }
    private ImportBatchResponse mapBatch(WorkforceImportBatch batch) {
        return new ImportBatchResponse(batch.getId(), batch.getFileName(), batch.getChecksum(), batch.getStatus(),
                headers(batch), decodeMapping(batch.getColumnMapping()), batch.getTotalRows(), batch.getValidRows(),
                batch.getInvalidRows(), batch.getImportedRows(), batch.getCreatedBy(), batch.getCreatedAt().toEpochMilli(),
                batch.getImportedAt() == null ? null : batch.getImportedAt().toEpochMilli(),
                batch.getReversedAt() == null ? null : batch.getReversedAt().toEpochMilli());
    }
    private List<String> headers(WorkforceImportBatch batch) {
        return batch.getHeadersText() == null || batch.getHeadersText().isBlank() ? List.of() : List.of(batch.getHeadersText().split("\\t", -1));
    }
    private List<String> readHeaders(byte[] bytes) throws Exception {
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() == 0) throw new BusinessRuleException("ملف Excel لا يحتوي على أوراق.");
            Row row = workbook.getSheetAt(0).getRow(workbook.getSheetAt(0).getFirstRowNum());
            if (row == null) throw new BusinessRuleException("صف العناوين غير موجود.");
            DataFormatter formatter = new DataFormatter(); List<String> headers = new ArrayList<>();
            for (int index = 0; index < row.getLastCellNum(); index++) headers.add(formatter.formatCellValue(row.getCell(index)).strip());
            return headers;
        }
    }
    private Map<String, Integer> headerIndexes(Row row) {
        DataFormatter formatter = new DataFormatter(); Map<String, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < row.getLastCellNum(); index++) result.put(formatter.formatCellValue(row.getCell(index)).strip(), index);
        return result;
    }
    private String encodeMapping(Map<String, String> mapping) {
        return REQUIRED_FIELDS.stream().map(field -> field + "\t" + mapping.get(field)).collect(java.util.stream.Collectors.joining("\n"));
    }
    private Map<String, String> decodeMapping(String encoded) {
        if (encoded == null || encoded.isBlank()) return Map.of(); Map<String, String> result = new LinkedHashMap<>();
        for (String line : encoded.split("\\n")) { String[] parts = line.split("\\t", 2); if (parts.length == 2) result.put(parts[0], parts[1]); }
        return result;
    }
    private String parseDate(Cell cell, DataFormatter formatter) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        String value = formatter.formatCellValue(cell).strip();
        for (DateTimeFormatter candidate : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("d/M/uuuu"), DateTimeFormatter.ofPattern("d-M-uuuu"))) {
            try { return LocalDate.parse(value, candidate).toString(); } catch (DateTimeParseException ignored) { }
        }
        return null;
    }
    private BigDecimal parseAttendance(String value) {
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "حاضر", "present", "1", "1.0", "1.00" -> BigDecimal.ONE;
            case "نصف", "نصف يوم", "half", "0.5", ".5" -> new BigDecimal("0.5");
            case "غائب", "absent", "0", "0.0", "0.00", "-", "—" -> BigDecimal.ZERO;
            default -> { try { yield new BigDecimal(normalized); } catch (Exception exception) { yield null; } }
        };
    }
    private boolean isBlank(Row row, DataFormatter formatter) {
        for (int index = row.getFirstCellNum(); index < row.getLastCellNum(); index++) if (!formatter.formatCellValue(row.getCell(index)).isBlank()) return false;
        return true;
    }
    private String text(Row row, Integer index, DataFormatter formatter) { return index == null ? "" : formatter.formatCellValue(row.getCell(index)); }
    private String raw(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>(); for (int i = 0; i < row.getLastCellNum(); i++) values.add(formatter.formatCellValue(row.getCell(i)));
        return String.join(" | ", values);
    }
    private String sha256(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private String actor() { var auth = SecurityContextHolder.getContext().getAuthentication(); return auth == null ? "system" : auth.getName(); }
    private String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
