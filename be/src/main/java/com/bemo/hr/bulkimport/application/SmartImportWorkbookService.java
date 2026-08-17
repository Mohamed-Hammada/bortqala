package com.bemo.hr.bulkimport.application;

import com.bemo.hr.bulkimport.domain.SmartImportModels.*;
import com.bemo.hr.bulkimport.domain.SmartImportModels.Sheet;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class SmartImportWorkbookService {
    private final SmartImportCatalog catalog;
    private final DataFormatter formatter = new DataFormatter(Locale.ROOT);

    public SmartImportWorkbookService(SmartImportCatalog catalog) {
        this.catalog = catalog;
    }

    static List<List<String>> parseCsvRecords(String input) {
        var records = new ArrayList<List<String>>();
        var record = new ArrayList<String>();
        var field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < input.length() && input.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                record.add(field.toString());
                field.setLength(0);
            } else if ((ch == '\n' || ch == '\r') && !quoted) {
                if (ch == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n') i++;
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else field.append(ch);
        }
        if (field.length() > 0 || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }

    public byte[] buildTemplate(Workflow workflow, boolean sample) {
        log.debug("buildTemplate called with workflowKey={}, sample={}", workflow.key(), sample);
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var headerStyle = headerStyle(workbook);
            var dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            for (var sheetSpec : workflow.sheets()) {
                var sheet = workbook.createSheet(safeSheetName(sheetSpec.titleEn()));
                var header = sheet.createRow(0);
                for (int c = 0; c < sheetSpec.columns().size(); c++) {
                    var column = sheetSpec.columns().get(c);
                    var cell = header.createCell(c);
                    cell.setCellValue(column.key());
                    cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(c, Math.min(60, Math.max(16, column.key().length() + 6)) * 256);
                    if (!column.allowedValues().isEmpty()) addDropdown(sheet, c, column.allowedValues());
                }
                sheet.createFreezePane(0, 1);
                if (sample) addSampleRow(workflow, sheetSpec, sheet, dateStyle);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not build import template", ex);
        }
    }

    public List<PreviewRow> parse(Workflow workflow, MultipartFile file) {
        log.debug("parse called with workflowKey={}, fileName={}", workflow.key(), file.getOriginalFilename());
        String name = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String lower = name.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".csv")) return parseCsv(workflow, file.getBytes());
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
                    return parseWorkbook(workflow, workbook);
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read uploaded file", ex);
        }
        throw new IllegalArgumentException("Only .xlsx, .xls, and .csv files are accepted.");
    }

    public byte[] rejectedWorkbook(Workflow workflow, List<PreviewRow> rows, List<CellError> errors) {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var headerStyle = headerStyle(workbook);
            var errorStyle = workbook.createCellStyle();
            errorStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var errorsByCell = new HashMap<String, CellError>();
            for (var error : errors)
                errorsByCell.put(error.sheet() + "#" + error.rowNumber() + "#" + error.column(), error);

            for (var spec : workflow.sheets()) {
                var rejected = rows.stream().filter(row -> row.sheet().equals(spec.key()) && !row.errors().isEmpty()).toList();
                if (rejected.isEmpty()) continue;
                var sheet = workbook.createSheet(safeSheetName(spec.titleEn()));
                var header = sheet.createRow(0);
                for (int c = 0; c < spec.columns().size(); c++) {
                    var cell = header.createCell(c);
                    cell.setCellValue(spec.columns().get(c).key());
                    cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(c, Math.min(60, Math.max(16, spec.columns().get(c).key().length() + 6)) * 256);
                }
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                CreationHelper helper = workbook.getCreationHelper();
                int outputRow = 1;
                for (var row : rejected) {
                    var excelRow = sheet.createRow(outputRow++);
                    for (int c = 0; c < spec.columns().size(); c++) {
                        var column = spec.columns().get(c);
                        var cell = excelRow.createCell(c);
                        cell.setCellValue(row.values().getOrDefault(column.key(), ""));
                        var error = errorsByCell.get(row.sheet() + "#" + row.rowNumber() + "#" + column.key());
                        if (error != null) {
                            cell.setCellStyle(errorStyle);
                            ClientAnchor anchor = helper.createClientAnchor();
                            anchor.setCol1(c);
                            anchor.setCol2(Math.min(c + 4, spec.columns().size()));
                            anchor.setRow1(excelRow.getRowNum());
                            anchor.setRow2(excelRow.getRowNum() + 4);
                            Comment comment = drawing.createCellComment(anchor);
                            comment.setAuthor("Bemo ERP Smart Import");
                            comment.setString(helper.createRichTextString(error.messageEn() + "\n" + error.messageAr()));
                            cell.setCellComment(comment);
                        }
                    }
                }
                sheet.createFreezePane(0, 1);
            }
            if (workbook.getNumberOfSheets() == 0) workbook.createSheet("Rejected Rows");
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not export rejected rows", ex);
        }
    }

    private List<PreviewRow> parseWorkbook(Workflow workflow, Workbook workbook) {
        var result = new ArrayList<PreviewRow>();
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            var excelSheet = workbook.getSheetAt(index);
            var spec = catalog.findSheet(workflow, excelSheet.getSheetName());
            if (spec == null && workflow.sheets().size() == 1 && index == 0) spec = workflow.sheets().get(0);
            if (spec == null) continue;
            var headerRow = excelSheet.getRow(excelSheet.getFirstRowNum());
            if (headerRow == null) continue;
            var columnIndex = mapHeaders(spec, headerRow);
            for (int r = headerRow.getRowNum() + 1; r <= excelSheet.getLastRowNum(); r++) {
                var row = excelSheet.getRow(r);
                if (row == null || isBlank(row)) continue;
                var values = new LinkedHashMap<String, String>();
                for (var column : spec.columns()) {
                    Integer c = columnIndex.get(column.key());
                    values.put(column.key(), c == null ? "" : cellText(row.getCell(c), column));
                }
                result.add(new PreviewRow(r + 1, spec.key(), values, List.of()));
            }
        }
        return result;
    }

    private Map<String, Integer> mapHeaders(Sheet spec, Row header) {
        var map = new LinkedHashMap<String, Integer>();
        for (Cell cell : header) {
            var incoming = formatter.formatCellValue(cell);
            var column = catalog.findColumn(spec, incoming);
            if (column != null) map.putIfAbsent(column.key(), cell.getColumnIndex());
        }
        return map;
    }

    private String cellText(Cell cell, Column column) {
        if (cell == null) return "";
        if (column.type() == com.bemo.hr.bulkimport.domain.SmartImportModels.ColumnType.DATE
                && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return formatter.formatCellValue(cell).strip();
    }

    private List<PreviewRow> parseCsv(Workflow workflow, byte[] bytes) {
        var spec = workflow.sheets().get(0);
        String text = new String(bytes, StandardCharsets.UTF_8).replace("\uFEFF", "");
        var lines = parseCsvRecords(text);
        if (lines.isEmpty()) return List.of();
        var headers = lines.get(0);
        var index = new LinkedHashMap<String, Integer>();
        for (int c = 0; c < headers.size(); c++) {
            var column = catalog.findColumn(spec, headers.get(c));
            if (column != null) index.putIfAbsent(column.key(), c);
        }
        var rows = new ArrayList<PreviewRow>();
        for (int r = 1; r < lines.size(); r++) {
            var record = lines.get(r);
            if (record.stream().allMatch(String::isBlank)) continue;
            var values = new LinkedHashMap<String, String>();
            for (var column : spec.columns()) {
                Integer c = index.get(column.key());
                values.put(column.key(), c == null || c >= record.size() ? "" : record.get(c).strip());
            }
            rows.add(new PreviewRow(r + 1, spec.key(), values, List.of()));
        }
        return rows;
    }

    private void addDropdown(org.apache.poi.ss.usermodel.Sheet sheet, int columnIndex, List<String> values) {
        String joined = String.join(",", values);
        if (joined.length() > 250) return;
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values.toArray(String[]::new));
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, 5000, columnIndex, columnIndex));
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private CellStyle headerStyle(Workbook workbook) {
        var style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void addSampleRow(Workflow workflow, Sheet spec, org.apache.poi.ss.usermodel.Sheet sheet, CellStyle dateStyle) {
        var row = sheet.createRow(1);
        for (int c = 0; c < spec.columns().size(); c++) {
            var column = spec.columns().get(c);
            var cell = row.createCell(c);
            String sample = sampleValue(workflow.key(), column.key(), column.allowedValues());
            cell.setCellValue(sample);
            if (column.type() == com.bemo.hr.bulkimport.domain.SmartImportModels.ColumnType.DATE)
                cell.setCellStyle(dateStyle);
        }
    }

    private String sampleValue(String workflow, String key, List<String> allowed) {
        if (!allowed.isEmpty()) return allowed.get(0);
        if (key.toLowerCase(Locale.ROOT).contains("date")) return LocalDate.now().toString();
        if (key.toLowerCase(Locale.ROOT).contains("amount") || key.toLowerCase(Locale.ROOT).contains("salary") || key.toLowerCase(Locale.ROOT).contains("quantity"))
            return "0.00";
        if (key.endsWith("Year")) return String.valueOf(LocalDate.now().getYear());
        if (key.endsWith("Code")) return "SAMPLE-001";
        if (key.equals("EmployeeCode")) return "EMP-001";
        return "";
    }

    private boolean isBlank(Row row) {
        for (Cell cell : row) if (!formatter.formatCellValue(cell).isBlank()) return false;
        return true;
    }

    private String safeSheetName(String value) {
        String sanitized = value.replaceAll("[\\\\/?*\\[\\]:]", " ").strip();
        return sanitized.length() <= 31 ? sanitized : sanitized.substring(0, 31);
    }
}
