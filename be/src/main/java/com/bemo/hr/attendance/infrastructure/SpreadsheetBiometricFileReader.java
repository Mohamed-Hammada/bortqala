package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.application.BiometricFileReader;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.healthmarketscience.jackcess.DatabaseBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;

@Component
public class SpreadsheetBiometricFileReader implements BiometricFileReader {
    private static final Set<String> EMPLOYEE_CODE_HEADERS = Set.of("employeecode", "كودالموظف");
    private static final Set<String> DAY_HEADERS = Set.of("day", "date", "workdate", "اليوم", "التاريخ");
    private static final Set<String> OFFICIAL_IN_HEADERS = Set.of("officialcheckin", "officialin", "الحضورالرسمي");
    private static final Set<String> OFFICIAL_OUT_HEADERS = Set.of("officialcheckout", "officialout", "الانصرافالرسمي");
    private static final Set<String> ACTUAL_IN_HEADERS = Set.of("actualcheckin", "actualin", "الحضورالفعلي");
    private static final Set<String> ACTUAL_OUT_HEADERS = Set.of("actualcheckout", "actualout", "الانصرافالفعلي");
    private final ZoneId companyZone;

    public SpreadsheetBiometricFileReader(@Value("${hr.company-zone:Africa/Cairo}") String companyZone) {
        this.companyZone = ZoneId.of(companyZone);
    }

    @Override
    public ParsedFile read(String fileName, InputStream inputStream) {
        var extension = extension(fileName);
        try {
            return switch (extension) {
                case "csv", "txt" -> readCsv(inputStream);
                case "xlsx", "xls" -> readWorkbook(inputStream);
                case "mdb", "accdb" -> readAccess(inputStream);
                default -> throw new BusinessRuleException("Supported biometric files are CSV, XLSX, XLS, MDB, and ACCDB.", "BIO_UNSUPPORTED_FORMAT", HttpStatus.CONFLICT);
            };
        } catch (IOException exception) {
            throw new BusinessRuleException("Could not read the biometric file.", "BIO_FILE_READ_FAILED", HttpStatus.CONFLICT);
        }
    }

    private ParsedFile readAccess(InputStream inputStream) throws IOException {
        var temporaryFile = Files.createTempFile("bemo-attendance-", ".mdb");
        try {
            Files.copy(inputStream, temporaryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try (var database = DatabaseBuilder.open(temporaryFile.toFile())) {
                if (!database.getTableNames().containsAll(Set.of("USERINFO", "CHECKINOUT"))) {
                    throw new BusinessRuleException("Access attendance backup must contain USERINFO and CHECKINOUT tables.", "BIO_ACCESS_TABLES_MISSING", HttpStatus.CONFLICT);
                }
                var users = database.getTable("USERINFO");
                var punches = database.getTable("CHECKINOUT");
                var userCodes = new HashMap<String, String>();
                var userNames = new HashMap<String, String>();
                for (var row : users) {
                    String userId = text(row.get("USERID"));
                    userCodes.put(userId, text(row.get("Badgenumber")));
                    userNames.put(userId, text(row.get("Name")));
                }
                var rows = new ArrayList<PunchRow>();
                var errors = new ArrayList<RowError>();
                int totalRows = 0;
                int importedRows = 0;
                for (var row : punches) {
                    totalRows++;
                    String userId = text(row.get("USERID"));
                    String deviceUserId = userCodes.getOrDefault(userId, "");
                    Object rawTime = row.get("CHECKTIME");
                    String rawLine = "USERID=" + safeRaw(userId) + " | CHECKTIME=" + safeRaw(rawTime);
                    try {
                        requireDeviceId(deviceUserId);
                        Instant punchedAt = accessInstant(rawTime);
                        rows.add(new PunchRow(totalRows, deviceUserId.strip(), blankToNull(userNames.get(userId)), punchedAt, rawLine));
                        importedRows++;
                    } catch (RuntimeException exception) {
                        errors.add(new RowError(totalRows, readableMessage(exception), rawLine));
                    }
                }
                return new ParsedFile(List.copyOf(rows), List.copyOf(errors), totalRows, importedRows);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private ParsedFile readCsv(InputStream inputStream) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            var headerLine = reader.readLine();
            if (headerLine == null) throw new BusinessRuleException("The biometric file is empty.", "BIO_FILE_MISSING", HttpStatus.CONFLICT);
            headerLine = headerLine.replace("\uFEFF", "");
            char delimiter = detectDelimiter(headerLine);
            var headers = indexHeaders(parseCsvLine(headerLine, delimiter));
            var rows = new ArrayList<PunchRow>();
            var errors = new ArrayList<RowError>();
            int totalRows = 0;
            int importedRows = 0;
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) continue;
                totalRows++;
                if (parseValues(rowNumber, parseCsvLine(line, delimiter), headers, line, rows, errors)) importedRows++;
            }
            return new ParsedFile(List.copyOf(rows), List.copyOf(errors), totalRows, importedRows);
        }
    }

    private ParsedFile readWorkbook(InputStream inputStream) throws IOException {
        try (var workbook = WorkbookFactory.create(inputStream)) {
            var sheet = workbook.getSheetAt(0);
            var headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) throw new BusinessRuleException("The biometric sheet is empty.", "BIO_SHEET_MISSING", HttpStatus.CONFLICT);
            var formatter = new DataFormatter(Locale.ROOT);
            var headerValues = new ArrayList<String>();
            for (int index = 0; index < headerRow.getLastCellNum(); index++) {
                headerValues.add(formatter.formatCellValue(headerRow.getCell(index)));
            }
            var headers = indexHeaders(headerValues);
            var rows = new ArrayList<PunchRow>();
            var errors = new ArrayList<RowError>();
            int totalRows = 0;
            int importedRows = 0;
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                var row = sheet.getRow(rowIndex);
                if (row == null || isEmpty(row, formatter)) continue;
                totalRows++;
                int rowNumber = rowIndex + 1;
                var values = new ArrayList<String>();
                for (int index = 0; index < headerRow.getLastCellNum(); index++) {
                    values.add(formatter.formatCellValue(row.getCell(index)));
                }
                String rawLine = String.join(" | ", values);
                try {
                    parseStructuredWorkbookRow(rowNumber, row, values, headers, formatter, rawLine, rows);
                    importedRows++;
                } catch (RuntimeException exception) {
                    errors.add(new RowError(rowNumber, readableMessage(exception), rawLine));
                }
            }
            return new ParsedFile(List.copyOf(rows), List.copyOf(errors), totalRows, importedRows);
        }
    }

    private boolean parseValues(int rowNumber, List<String> values, Map<String, Integer> headers, String rawLine,
                                List<PunchRow> rows, List<RowError> errors) {
        try {
            parseStructuredValues(rowNumber, values, headers, rawLine, rows);
            return true;
        } catch (RuntimeException exception) {
            errors.add(new RowError(rowNumber, readableMessage(exception), rawLine));
            return false;
        }
    }

    private Map<String, Integer> indexHeaders(List<String> headers) {
        var result = new HashMap<String, Integer>();
        for (int index = 0; index < headers.size(); index++) {
            var normalized = normalize(headers.get(index));
            if (EMPLOYEE_CODE_HEADERS.contains(normalized)) result.putIfAbsent("employeeCode", index);
            if (DAY_HEADERS.contains(normalized)) result.putIfAbsent("day", index);
            if (OFFICIAL_IN_HEADERS.contains(normalized)) result.putIfAbsent("officialIn", index);
            if (OFFICIAL_OUT_HEADERS.contains(normalized)) result.putIfAbsent("officialOut", index);
            if (ACTUAL_IN_HEADERS.contains(normalized)) result.putIfAbsent("actualIn", index);
            if (ACTUAL_OUT_HEADERS.contains(normalized)) result.putIfAbsent("actualOut", index);
        }
        for (int index = 0; index < headers.size(); index++) {
            var normalized = normalize(headers.get(index));
            if (Set.of("deviceuserid", "deviceuser", "badge", "badgenumber").contains(normalized)) result.putIfAbsent("deviceUserId", index);
            if (Set.of("punchedat", "checktime", "timestamp", "datetime").contains(normalized)) result.putIfAbsent("punchedAt", index);
            if (Set.of("employeename", "name", "username").contains(normalized)) result.putIfAbsent("employeeName", index);
        }
        if (result.containsKey("deviceUserId") && result.containsKey("punchedAt")) return result;
        var structuredKeys = Set.of("employeeCode", "day", "officialIn", "officialOut", "actualIn", "actualOut");
        if (!result.keySet().containsAll(structuredKeys)) {
            throw new BusinessRuleException(
                    "Required columns: Employee code, Day, Official check-in, Official check-out, Actual check-in, Actual check-out. "
                            + "/ الأعمدة المطلوبة: كود الموظف، اليوم، الحضور الرسمي، الانصراف الرسمي، الحضور الفعلي، الانصراف الفعلي.",
                    "BIO_REQUIRED_COLUMNS_MISSING", HttpStatus.CONFLICT);
        }
        return result;
    }

    private void parseStructuredWorkbookRow(int rowNumber, Row row, List<String> values,
                                            Map<String, Integer> headers, DataFormatter formatter,
                                            String rawLine, List<PunchRow> rows) {
        if (headers.containsKey("deviceUserId")) {
            String deviceUserId = value(values, headers.get("deviceUserId"));
            requireDeviceId(deviceUserId);
            Instant punchedAt = excelInstant(row.getCell(headers.get("punchedAt")), formatter);
            rows.add(new PunchRow(rowNumber, deviceUserId.strip(), blankToNull(value(values, headers.get("employeeName"))), punchedAt, rawLine));
            return;
        }
        String employeeCode = value(values, headers.get("employeeCode"));
        requireDeviceId(employeeCode);
        LocalDate day = excelDate(row.getCell(headers.get("day")), formatter);
        excelTime(row.getCell(headers.get("officialIn")), formatter, true);
        excelTime(row.getCell(headers.get("officialOut")), formatter, true);
        LocalTime actualIn = excelTime(row.getCell(headers.get("actualIn")), formatter, false);
        LocalTime actualOut = excelTime(row.getCell(headers.get("actualOut")), formatter, false);
        addActualPunches(rowNumber, employeeCode, day, actualIn, actualOut, rawLine, rows);
    }

    private void parseStructuredValues(int rowNumber, List<String> values, Map<String, Integer> headers,
                                       String rawLine, List<PunchRow> rows) {
        if (headers.containsKey("deviceUserId")) {
            String deviceUserId = value(values, headers.get("deviceUserId"));
            requireDeviceId(deviceUserId);
            rows.add(new PunchRow(rowNumber, deviceUserId.strip(), blankToNull(value(values, headers.get("employeeName"))),
                    parseInstant(value(values, headers.get("punchedAt"))), rawLine));
            return;
        }
        String employeeCode = value(values, headers.get("employeeCode"));
        requireDeviceId(employeeCode);
        LocalDate day = parseDate(value(values, headers.get("day")));
        parseTime(value(values, headers.get("officialIn")), true);
        parseTime(value(values, headers.get("officialOut")), true);
        LocalTime actualIn = parseTime(value(values, headers.get("actualIn")), false);
        LocalTime actualOut = parseTime(value(values, headers.get("actualOut")), false);
        addActualPunches(rowNumber, employeeCode, day, actualIn, actualOut, rawLine, rows);
    }

    private void addActualPunches(int rowNumber, String employeeCode, LocalDate day, LocalTime actualIn,
                                  LocalTime actualOut, String rawLine, List<PunchRow> rows) {
        if (actualIn != null) rows.add(new PunchRow(rowNumber, employeeCode.strip(), null,
                day.atTime(actualIn).atZone(companyZone).toInstant(), rawLine));
        if (actualOut != null && !actualOut.equals(actualIn)) {
            LocalDate outDay = actualIn != null && actualOut.isBefore(actualIn) ? day.plusDays(1) : day;
            rows.add(new PunchRow(rowNumber, employeeCode.strip(), null,
                    outDay.atTime(actualOut).atZone(companyZone).toInstant(), rawLine));
        }
    }

    private LocalDate excelDate(Cell cell, DataFormatter formatter) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            double numeric = cell.getNumericCellValue();
            long whole = Math.round(numeric);
            if (Math.abs(numeric - whole) < 0.000001 && whole >= 19000101 && whole <= 99991231) {
                return LocalDate.parse(Long.toString(whole), DateTimeFormatter.BASIC_ISO_DATE);
            }
            if (DateUtil.isValidExcelDate(numeric)) {
                return DateUtil.getLocalDateTime(numeric).toLocalDate();
            }
        }
        return parseDate(formatter.formatCellValue(cell));
    }

    private LocalTime excelTime(Cell cell, DataFormatter formatter, boolean required) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalTime().withNano(0);
        }
        return parseTime(formatter.formatCellValue(cell), required);
    }

    private Instant excelInstant(Cell cell, DataFormatter formatter) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
            return DateUtil.getLocalDateTime(cell.getNumericCellValue()).atZone(companyZone).toInstant();
        }
        return parseInstant(formatter.formatCellValue(cell));
    }

    private Instant parseInstant(String value) {
        String normalized = value == null ? "" : value.strip();
        for (var formatter : List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"), DateTimeFormatter.ofPattern("d/M/yyyy H:mm:ss"),
                DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.ENGLISH))) {
            try { return java.time.LocalDateTime.parse(normalized, formatter).atZone(companyZone).toInstant(); }
            catch (DateTimeParseException ignored) { }
        }
        try { return Instant.parse(normalized); }
        catch (DateTimeParseException ignored) { throw new IllegalArgumentException("Invalid punch timestamp."); }
    }

    private Instant accessInstant(Object value) {
        if (value instanceof java.time.LocalDateTime localDateTime) return localDateTime.atZone(companyZone).toInstant();
        if (value instanceof java.util.Date date) return date.toInstant();
        return parseInstant(text(value));
    }

    private static String text(Object value) { return value == null ? "" : value.toString(); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private static String safeRaw(Object value) { return text(value).replace('|', '/'); }

    private LocalDate parseDate(String value) {
        String normalized = value.strip();
        for (var formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"), DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("yyyy/M/d"))) {
            try { return LocalDate.parse(normalized, formatter); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("قيمة التاريخ غير صحيحة. استخدم تاريخ Excel أو صيغة يوم/شهر/سنة.");
    }

    private LocalTime parseTime(String value, boolean required) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            if (required) throw new IllegalArgumentException("Official attendance time is required.");
            return null;
        }
        for (var formatter : List.of(DateTimeFormatter.ISO_LOCAL_TIME, DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))) {
            try { return LocalTime.parse(normalized, formatter).withNano(0); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("Invalid attendance time.");
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        var values = new ArrayList<String>();
        var value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else quoted = !quoted;
            } else if (current == delimiter && !quoted) {
                values.add(value.toString().strip());
                value.setLength(0);
            } else value.append(current);
        }
        values.add(value.toString().strip());
        return values;
    }

    private char detectDelimiter(String header) {
        if (header.chars().filter(character -> character == '\t').count() > 1) return '\t';
        if (header.chars().filter(character -> character == ';').count() > header.chars().filter(character -> character == ',').count()) return ';';
        return ',';
    }

    private boolean isEmpty(Row row, DataFormatter formatter) {
        for (var cell : row) if (!formatter.formatCellValue(cell).isBlank()) return false;
        return true;
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-]+", "");
    }

    private String value(List<String> values, Integer index) {
        if (index == null || index >= values.size()) return "";
        return values.get(index);
    }

    private void requireDeviceId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Device user id is required.");
    }

    private String readableMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "Invalid row." : exception.getMessage();
    }

    private String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
