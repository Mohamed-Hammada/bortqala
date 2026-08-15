package com.bemo.hr.shared.dataexchange;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DataExchangeWorkbookService {
    private static final int MAX_IMPORT_ROWS = 5_000;
    private static final long MAX_IMPORT_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_EXPORT_ROWS = 25_000;
    private static final int MAX_EXPORT_COLUMNS = 100;

    private final SpreadsheetTemplateCatalog catalog;

    public DataExchangeWorkbookService(SpreadsheetTemplateCatalog catalog) {
        this.catalog = catalog;
    }

    public List<TemplateSummary> catalog() {
        return catalog.all().stream().map(definition -> new TemplateSummary(
                definition.key(),
                definition.module(),
                definition.title(),
                definition.workspaceRoute(),
                definition.description(),
                definition.commitSupported(),
                definition.sheets().stream().map(sheet -> new SheetSummary(
                        sheet.name(),
                        sheet.columns().stream().map(column -> new ColumnSummary(
                                column.header(), column.required(), column.type().name(),
                                column.uniqueWithinFile(), column.allowedValues(), column.description())).toList()
                )).toList()
        )).toList();
    }

    public byte[] createTemplate(String key, boolean includeSample) {
        SpreadsheetTemplateDefinition definition = catalog.require(key);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle requiredHeaderStyle = requiredHeaderStyle(workbook);
            CellStyle hintStyle = hintStyle(workbook);

            for (SpreadsheetTemplateDefinition.SheetDefinition sheetDefinition : definition.sheets()) {
                Sheet sheet = workbook.createSheet(safeSheetName(sheetDefinition.name()));
                Row header = sheet.createRow(0);
                Row hints = sheet.createRow(1);

                for (int i = 0; i < sheetDefinition.columns().size(); i++) {
                    SpreadsheetTemplateDefinition.ColumnDefinition column = sheetDefinition.columns().get(i);
                    Cell cell = header.createCell(i);
                    cell.setCellValue(column.header());
                    cell.setCellStyle(column.required() ? requiredHeaderStyle : headerStyle);

                    Cell hint = hints.createCell(i);
                    String required = column.required() ? "Required" : "Optional";
                    String allowed = column.allowedValues().isEmpty()
                            ? ""
                            : " | Allowed: " + String.join(", ", column.allowedValues());
                    hint.setCellValue(required + " | " + column.type().name() + allowed + " | " + column.description());
                    hint.setCellStyle(hintStyle);

                    if (!column.allowedValues().isEmpty()) {
                        addListValidation(sheet, i, column.allowedValues());
                    }
                    sheet.setColumnWidth(i, Math.min(60, Math.max(16, column.header().length() + 5)) * 256);
                }

                sheet.createFreezePane(0, 2);
                if (includeSample && !sheetDefinition.sampleRows().isEmpty()) {
                    int rowIndex = 2;
                    for (Map<String, Object> sample : sheetDefinition.sampleRows()) {
                        Row row = sheet.createRow(rowIndex++);
                        for (int i = 0; i < sheetDefinition.columns().size(); i++) {
                            SpreadsheetTemplateDefinition.ColumnDefinition column = sheetDefinition.columns().get(i);
                            Object value = sample.get(column.header());
                            if (value != null) {
                                setCellValue(row.createCell(i), value);
                            }
                        }
                    }
                }
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate spreadsheet template", exception);
        }
    }

    public ValidationResult validate(String key, MultipartFile file) {
        SpreadsheetTemplateDefinition definition = catalog.require(key);
        validateFile(file);

        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            List<SheetValidationResult> sheetResults = new ArrayList<>();
            int totalRows = 0;
            int validRows = 0;
            int invalidRows = 0;

            for (int sheetIndex = 0; sheetIndex < definition.sheets().size(); sheetIndex++) {
                SpreadsheetTemplateDefinition.SheetDefinition expected = definition.sheets().get(sheetIndex);
                Sheet actual = findSheet(workbook, expected.name(), sheetIndex);
                SheetValidationResult result = validateSheet(expected, actual);
                sheetResults.add(result);
                totalRows += result.totalRows();
                validRows += result.validRows();
                invalidRows += result.invalidRows();
            }

            boolean valid = invalidRows == 0 && sheetResults.stream().allMatch(result -> result.sheetErrors().isEmpty());
            return new ValidationResult(key, valid, totalRows, validRows, invalidRows, sheetResults);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read workbook: " + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse workbook", exception);
        }
    }

    public byte[] createErrorWorkbook(String key, MultipartFile file) {
        SpreadsheetTemplateDefinition definition = catalog.require(key);
        ValidationResult validation = validate(key, file);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = requiredHeaderStyle(workbook);
            CellStyle errorStyle = errorCellStyle(workbook);
            CreationHelper helper = workbook.getCreationHelper();

            for (int sheetIndex = 0; sheetIndex < definition.sheets().size(); sheetIndex++) {
                SpreadsheetTemplateDefinition.SheetDefinition sheetDefinition = definition.sheets().get(sheetIndex);
                SheetValidationResult validationSheet = validation.sheets().get(sheetIndex);
                Sheet sheet = workbook.createSheet(safeSheetName(sheetDefinition.name()));
                Row header = sheet.createRow(0);
                for (int i = 0; i < sheetDefinition.columns().size(); i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(sheetDefinition.columns().get(i).header());
                    cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(i, Math.min(60, Math.max(16, sheetDefinition.columns().get(i).header().length() + 5)) * 256);
                }
                Cell errorHeader = header.createCell(sheetDefinition.columns().size());
                errorHeader.setCellValue("_RowErrors");
                errorHeader.setCellStyle(headerStyle);
                sheet.setColumnWidth(sheetDefinition.columns().size(), 70 * 256);

                int targetRowIndex = 1;
                for (RowValidationResult rejected : validationSheet.rows()) {
                    if (rejected.errors().isEmpty()) {
                        continue;
                    }
                    Row row = sheet.createRow(targetRowIndex++);
                    Map<String, List<String>> errorsByColumn = new HashMap<>();
                    for (CellValidationError error : rejected.errors()) {
                        errorsByColumn.computeIfAbsent(error.column(), ignored -> new ArrayList<>()).add(error.message());
                    }

                    for (int i = 0; i < sheetDefinition.columns().size(); i++) {
                        String headerName = sheetDefinition.columns().get(i).header();
                        Cell cell = row.createCell(i);
                        cell.setCellValue(rejected.values().getOrDefault(headerName, ""));
                        List<String> errors = errorsByColumn.get(headerName);
                        if (errors != null && !errors.isEmpty()) {
                            cell.setCellStyle(errorStyle);
                            Drawing<?> drawing = sheet.createDrawingPatriarch();
                            ClientAnchor anchor = helper.createClientAnchor();
                            anchor.setCol1(i);
                            anchor.setCol2(Math.min(i + 3, sheetDefinition.columns().size()));
                            anchor.setRow1(targetRowIndex - 1);
                            anchor.setRow2(targetRowIndex + 3);
                            Comment comment = drawing.createCellComment(anchor);
                            comment.setString(helper.createRichTextString(String.join("\n", errors)));
                            comment.setAuthor("Bemo ERP validation");
                            cell.setCellComment(comment);
                        }
                    }
                    Cell errorsCell = row.createCell(sheetDefinition.columns().size());
                    errorsCell.setCellValue(rejected.errors().stream()
                            .map(error -> error.column() + ": " + error.message())
                            .reduce((left, right) -> left + " | " + right).orElse(""));
                }

                if (!validationSheet.sheetErrors().isEmpty()) {
                    int rowIndex = Math.max(targetRowIndex + 1, 2);
                    Row row = sheet.createRow(rowIndex);
                    Cell cell = row.createCell(0);
                    cell.setCellValue("Sheet errors: " + String.join(" | ", validationSheet.sheetErrors()));
                    cell.setCellStyle(errorStyle);
                }
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate error workbook", exception);
        }
    }

    public byte[] createExport(ExportWorkbookRequest request) {
        if (request == null || request.columns() == null || request.columns().isEmpty()) {
            throw new IllegalArgumentException("At least one export column is required");
        }
        if (request.columns().size() > MAX_EXPORT_COLUMNS) {
            throw new IllegalArgumentException("Export exceeds the maximum column count");
        }
        List<Map<String, Object>> rows = request.rows() == null ? List.of() : request.rows();
        if (rows.size() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("Export exceeds the maximum row count");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(blankToDefault(request.sheetName(), "Export")));
            CellStyle headerStyle = requiredHeaderStyle(workbook);
            Row header = sheet.createRow(0);
            for (int i = 0; i < request.columns().size(); i++) {
                ExportColumn column = request.columns().get(i);
                Cell cell = header.createCell(i);
                cell.setCellValue(blankToDefault(column.header(), column.key()));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.min(60, Math.max(14, blankToDefault(column.header(), column.key()).length() + 4)) * 256);
            }
            sheet.createFreezePane(0, 1);

            int rowIndex = 1;
            for (Map<String, Object> sourceRow : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < request.columns().size(); i++) {
                    ExportColumn column = request.columns().get(i);
                    Object value = sourceRow == null ? null : sourceRow.get(column.key());
                    if (value != null) {
                        setCellValue(row.createCell(i), value);
                    }
                }
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate export workbook", exception);
        }
    }

    private SheetValidationResult validateSheet(SpreadsheetTemplateDefinition.SheetDefinition expected, Sheet actual) {
        if (actual == null) {
            return new SheetValidationResult(expected.name(), 0, 0, 0,
                    List.of("Missing required sheet: " + expected.name()), List.of());
        }

        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        Row headerRow = actual.getRow(actual.getFirstRowNum());
        if (headerRow == null) {
            return new SheetValidationResult(expected.name(), 0, 0, 0,
                    List.of("Missing header row"), List.of());
        }

        Map<String, Integer> headerIndexes = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String value = formatter.formatCellValue(cell).strip();
            if (!value.isBlank()) {
                headerIndexes.put(value.toLowerCase(Locale.ROOT), cell.getColumnIndex());
            }
        }

        List<String> sheetErrors = new ArrayList<>();
        for (SpreadsheetTemplateDefinition.ColumnDefinition column : expected.columns()) {
            if (column.required() && !headerIndexes.containsKey(column.header().toLowerCase(Locale.ROOT))) {
                sheetErrors.add("Missing required column: " + column.header());
            }
        }

        Map<String, Set<String>> uniqueValues = new HashMap<>();
        expected.columns().stream().filter(SpreadsheetTemplateDefinition.ColumnDefinition::uniqueWithinFile)
                .forEach(column -> uniqueValues.put(column.header(), new HashSet<>()));

        List<RowValidationResult> rows = new ArrayList<>();
        int dataRows = 0;
        int validRows = 0;
        int invalidRows = 0;
        int firstDataRow = headerRow.getRowNum() + 1;
        // Generated templates include a metadata/hint row. Skip it when it starts with Required/Optional.
        Row possibleHint = actual.getRow(firstDataRow);
        if (possibleHint != null && looksLikeHintRow(possibleHint, formatter)) {
            firstDataRow++;
        }

        for (int rowIndex = firstDataRow; rowIndex <= actual.getLastRowNum(); rowIndex++) {
            Row row = actual.getRow(rowIndex);
            if (row == null || isBlank(row, formatter)) {
                continue;
            }
            dataRows++;
            if (dataRows > MAX_IMPORT_ROWS) {
                sheetErrors.add("Maximum import rows exceeded: " + MAX_IMPORT_ROWS);
                break;
            }

            Map<String, String> values = new LinkedHashMap<>();
            List<CellValidationError> errors = new ArrayList<>();

            for (SpreadsheetTemplateDefinition.ColumnDefinition column : expected.columns()) {
                Integer index = headerIndexes.get(column.header().toLowerCase(Locale.ROOT));
                String value = index == null ? "" : formatter.formatCellValue(row.getCell(index)).strip();
                values.put(column.header(), value);

                if (column.required() && value.isBlank()) {
                    errors.add(new CellValidationError(column.header(), "Required value is missing"));
                    continue;
                }
                if (value.isBlank()) {
                    continue;
                }

                String typeError = validateType(column.type(), value);
                if (typeError != null) {
                    errors.add(new CellValidationError(column.header(), typeError));
                }
                if (!column.allowedValues().isEmpty()
                        && column.allowedValues().stream().noneMatch(allowed -> allowed.equalsIgnoreCase(value))) {
                    errors.add(new CellValidationError(column.header(),
                            "Value must be one of: " + String.join(", ", column.allowedValues())));
                }
                if (column.uniqueWithinFile()) {
                    Set<String> seen = uniqueValues.get(column.header());
                    String normalized = value.toLowerCase(Locale.ROOT);
                    if (!seen.add(normalized)) {
                        errors.add(new CellValidationError(column.header(), "Duplicate value within uploaded file"));
                    }
                }
            }

            if (errors.isEmpty()) {
                validRows++;
            } else {
                invalidRows++;
            }
            rows.add(new RowValidationResult(rowIndex + 1, values, errors));
        }

        return new SheetValidationResult(expected.name(), dataRows, validRows, invalidRows, sheetErrors, rows);
    }

    private String validateType(SpreadsheetTemplateDefinition.ColumnType type, String value) {
        try {
            switch (type) {
                case INTEGER -> Integer.parseInt(normalizeNumber(value));
                case DECIMAL -> new BigDecimal(normalizeNumber(value));
                case DATE -> LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                case DATETIME -> parseDateTime(value);
                case BOOLEAN -> {
                    if (!(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")
                            || value.equals("1") || value.equals("0")
                            || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("no"))) {
                        return "Expected boolean value (true/false, yes/no, 1/0)";
                    }
                }
                case TEXT -> { }
            }
            return null;
        } catch (RuntimeException exception) {
            return switch (type) {
                case INTEGER -> "Expected integer value";
                case DECIMAL -> "Expected decimal number";
                case DATE -> "Expected ISO date YYYY-MM-DD";
                case DATETIME -> "Expected ISO date/time YYYY-MM-DDTHH:mm:ss";
                case BOOLEAN -> "Expected boolean value";
                case TEXT -> null;
            };
        }
    }

    private void parseDateTime(String value) {
        try {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException first) {
            LocalDateTime.parse(value.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private String normalizeNumber(String value) {
        return value.replace(",", "").strip();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet file is required");
        }
        if (file.getSize() > MAX_IMPORT_FILE_BYTES) {
            throw new IllegalArgumentException("Spreadsheet file must not exceed 10 MB");
        }
        String filename = file.getOriginalFilename() == null ? "upload.xlsx" : file.getOriginalFilename();
        if (!filename.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$")) {
            throw new IllegalArgumentException("Spreadsheet file must be .xlsx or .xls");
        }
    }

    private Sheet findSheet(Workbook workbook, String expectedName, int expectedIndex) {
        Sheet byName = workbook.getSheet(expectedName);
        if (byName != null) {
            return byName;
        }
        return expectedIndex < workbook.getNumberOfSheets() ? workbook.getSheetAt(expectedIndex) : null;
    }

    private boolean looksLikeHintRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            String value = formatter.formatCellValue(cell).strip();
            if (value.startsWith("Required |") || value.startsWith("Optional |")) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private void addListValidation(Sheet sheet, int columnIndex, List<String> allowedValues) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(allowedValues.toArray(String[]::new));
        CellRangeAddressList addressList = new CellRangeAddressList(2, MAX_IMPORT_ROWS + 1, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle requiredHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle hintStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle errorCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setCellValue(Cell cell, Object value) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private String safeSheetName(String value) {
        String safe = value == null ? "Data" : value.replaceAll("[\\\\/?*\\[\\]:]", " ").strip();
        if (safe.isBlank()) safe = "Data";
        return safe.length() > 31 ? safe.substring(0, 31) : safe;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record TemplateSummary(
            String key,
            String module,
            String title,
            String workspaceRoute,
            String description,
            boolean commitSupported,
            List<SheetSummary> sheets) {
    }

    public record SheetSummary(String name, List<ColumnSummary> columns) {
    }

    public record ColumnSummary(
            String header,
            boolean required,
            String type,
            boolean uniqueWithinFile,
            List<String> allowedValues,
            String description) {
    }

    public record ValidationResult(
            String templateKey,
            boolean valid,
            int totalRows,
            int validRows,
            int invalidRows,
            List<SheetValidationResult> sheets) {
    }

    public record SheetValidationResult(
            String sheet,
            int totalRows,
            int validRows,
            int invalidRows,
            List<String> sheetErrors,
            List<RowValidationResult> rows) {
    }

    public record RowValidationResult(
            int rowNumber,
            Map<String, String> values,
            List<CellValidationError> errors) {
    }

    public record CellValidationError(String column, String message) {
    }

    public record ExportColumn(String key, String header) {
    }

    public record ExportWorkbookRequest(
            String filename,
            String sheetName,
            List<ExportColumn> columns,
            List<Map<String, Object>> rows) {
    }
}
