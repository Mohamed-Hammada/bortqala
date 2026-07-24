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
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SpreadsheetBiometricFileReader implements BiometricFileReader {
    private static final Set<String> DEVICE_HEADERS = Set.of("deviceuserid", "userid", "userno", "employeeid", "الرقم", "رقمالموظف");
    private static final Set<String> NAME_HEADERS = Set.of("employeename", "name", "username", "الاسم", "اسمالموظف");
    private static final Set<String> TIME_HEADERS = Set.of("punchedat", "punchtime", "datetime", "timestamp", "time", "التاريخوالوقت", "وقتالبصمة");
    private static final List<DateTimeFormatter> LOCAL_DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm"));

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
                default -> throw new BusinessRuleException("Supported biometric files are CSV, XLSX, and XLS.");
            };
        } catch (IOException exception) {
            throw new BusinessRuleException("Could not read the biometric file.");
        }
    }

    private ParsedFile readCsv(InputStream inputStream) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            var headerLine = reader.readLine();
            if (headerLine == null) throw new BusinessRuleException("The biometric file is empty.");
            headerLine = headerLine.replace("\uFEFF", "");
            char delimiter = detectDelimiter(headerLine);
            var headers = indexHeaders(parseCsvLine(headerLine, delimiter));
            var rows = new ArrayList<PunchRow>();
            var errors = new ArrayList<RowError>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) continue;
                parseValues(rowNumber, parseCsvLine(line, delimiter), headers, line, rows, errors);
            }
            return new ParsedFile(List.copyOf(rows), List.copyOf(errors), rows.size() + errors.size());
        }
    }

    private ParsedFile readWorkbook(InputStream inputStream) throws IOException {
        try (var workbook = WorkbookFactory.create(inputStream)) {
            var sheet = workbook.getSheetAt(0);
            var headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) throw new BusinessRuleException("The biometric sheet is empty.");
            var formatter = new DataFormatter(Locale.ROOT);
            var headerValues = new ArrayList<String>();
            for (int index = 0; index < headerRow.getLastCellNum(); index++) {
                headerValues.add(formatter.formatCellValue(headerRow.getCell(index)));
            }
            var headers = indexHeaders(headerValues);
            var rows = new ArrayList<PunchRow>();
            var errors = new ArrayList<RowError>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                var row = sheet.getRow(rowIndex);
                if (row == null || isEmpty(row, formatter)) continue;
                int rowNumber = rowIndex + 1;
                var values = new ArrayList<String>();
                for (int index = 0; index < headerRow.getLastCellNum(); index++) {
                    values.add(formatter.formatCellValue(row.getCell(index)));
                }
                String rawLine = String.join(" | ", values);
                try {
                    String deviceId = value(values, headers.get("device"));
                    String name = nullableValue(values, headers.get("name"));
                    var timeCell = row.getCell(headers.get("time"));
                    Instant punchedAt = excelInstant(timeCell, formatter);
                    requireDeviceId(deviceId);
                    rows.add(new PunchRow(rowNumber, deviceId.strip(), name, punchedAt, rawLine));
                } catch (RuntimeException exception) {
                    errors.add(new RowError(rowNumber, readableMessage(exception), rawLine));
                }
            }
            return new ParsedFile(List.copyOf(rows), List.copyOf(errors), rows.size() + errors.size());
        }
    }

    private void parseValues(int rowNumber, List<String> values, Map<String, Integer> headers, String rawLine,
                             List<PunchRow> rows, List<RowError> errors) {
        try {
            String deviceId = value(values, headers.get("device"));
            requireDeviceId(deviceId);
            String name = nullableValue(values, headers.get("name"));
            Instant punchedAt = parseInstant(value(values, headers.get("time")));
            rows.add(new PunchRow(rowNumber, deviceId.strip(), name, punchedAt, rawLine));
        } catch (RuntimeException exception) {
            errors.add(new RowError(rowNumber, readableMessage(exception), rawLine));
        }
    }

    private Map<String, Integer> indexHeaders(List<String> headers) {
        var result = new HashMap<String, Integer>();
        for (int index = 0; index < headers.size(); index++) {
            var normalized = normalize(headers.get(index));
            if (DEVICE_HEADERS.contains(normalized)) result.putIfAbsent("device", index);
            if (NAME_HEADERS.contains(normalized)) result.putIfAbsent("name", index);
            if (TIME_HEADERS.contains(normalized)) result.putIfAbsent("time", index);
        }
        if (!result.containsKey("device") || !result.containsKey("time")) {
            throw new BusinessRuleException("Required columns: device_user_id and punched_at. employee_name is optional.");
        }
        return result;
    }

    private Instant excelInstant(Cell cell, DataFormatter formatter) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().atZone(companyZone).toInstant();
        }
        return parseInstant(formatter.formatCellValue(cell));
    }

    private Instant parseInstant(String value) {
        String normalized = value.strip();
        try { return Instant.parse(normalized); } catch (DateTimeParseException ignored) { }
        try { return OffsetDateTime.parse(normalized).toInstant(); } catch (DateTimeParseException ignored) { }
        for (var formatter : LOCAL_DATE_TIME_FORMATS) {
            try { return LocalDateTime.parse(normalized, formatter).atZone(companyZone).toInstant(); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("Invalid punch date/time.");
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

    private String nullableValue(List<String> values, Integer index) {
        String value = value(values, index).strip();
        return value.isBlank() ? null : value;
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
