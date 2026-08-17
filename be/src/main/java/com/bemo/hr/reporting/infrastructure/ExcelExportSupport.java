package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.i18n.TranslationService;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExcelExportSupport {
    private ExcelExportSupport() {
    }

    public static Map<String, String> messages(TranslationService translationService, ExcelExportOptions options) {
        return translationService.bundle(options.locale()).messages();
    }

    public static String text(Map<String, String> messages, String key) {
        return messages.getOrDefault(key, key);
    }

    public static String enumText(Map<String, String> messages, Enum<?> value) {
        if (value == null) return "";
        String key = "export.value." + value.name().toLowerCase(Locale.ROOT);
        return messages.getOrDefault(key, value.name().replace('_', ' '));
    }

    public static Styles styles(XSSFWorkbook workbook) {
        var textStyle = workbook.createCellStyle();
        textStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

        var integerStyle = workbook.createCellStyle();
        integerStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        integerStyle.setAlignment(HorizontalAlignment.RIGHT);

        var dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

        var dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

        return new Styles(textStyle, integerStyle, dateStyle, dateTimeStyle);
    }

    public static XSSFSheet sheet(XSSFWorkbook workbook, String name, boolean rightToLeft) {
        var sheet = workbook.createSheet(name);
        sheet.setRightToLeft(rightToLeft);
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    public static void writeHeader(XSSFSheet sheet, List<String> headers) {
        var row = sheet.createRow(0);
        row.setHeightInPoints(24);
        for (int column = 0; column < headers.size(); column++) {
            row.createCell(column).setCellValue(headers.get(column));
        }
    }

    public static void writeRow(XSSFSheet sheet, int rowIndex, List<?> values, Styles styles) {
        var row = sheet.createRow(rowIndex);
        row.setHeightInPoints(21);
        for (int column = 0; column < values.size(); column++) {
            var cell = row.createCell(column);
            var value = values.get(column);
            if (value == null) {
                cell.setBlank();
                cell.setCellStyle(styles.text());
            } else if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
                cell.setCellStyle(styles.integer());
            } else if (value instanceof LocalDate date) {
                cell.setCellValue(java.sql.Date.valueOf(date));
                cell.setCellStyle(styles.date());
            } else if (value instanceof Instant instant) {
                cell.setCellValue(Date.from(instant));
                cell.setCellStyle(styles.dateTime());
            } else if (value instanceof CharSequence text && text.toString().isBlank()) {
                cell.setBlank();
                cell.setCellStyle(styles.text());
            } else {
                cell.setCellValue(escapeFormula(String.valueOf(value)));
                cell.setCellStyle(styles.text());
            }
        }
    }

    /**
     * Prevents spreadsheet formula injection (guide §5.6): user-controlled text cells
     * beginning with {@code =}, {@code +}, {@code -}, or {@code @} are prefixed with an
     * apostrophe so they are treated as literal text by Excel/Sheets instead of formulas.
     */
    public static String escapeFormula(String value) {
        if (value == null || value.isEmpty()) return value;
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }

    public static void finishTable(XSSFSheet sheet, int lastDataRow, int columnCount, String tableName,
                                   ExcelExportOptions options) {
        int tableLastRow = Math.max(1, lastDataRow);
        if (lastDataRow == 0) sheet.createRow(1);
        var area = new AreaReference(new CellReference(0, 0),
                new CellReference(tableLastRow, columnCount - 1), SpreadsheetVersion.EXCEL2007);
        var table = sheet.createTable(area);
        table.setName(tableName);
        table.setDisplayName(tableName);
        var style = table.getCTTable().isSetTableStyleInfo()
                ? table.getCTTable().getTableStyleInfo() : table.getCTTable().addNewTableStyleInfo();
        style.setName(options.tableStyle().poiStyleName());
        style.setShowRowStripes(true);
        style.setShowColumnStripes(false);
        style.setShowFirstColumn(false);
        style.setShowLastColumn(false);

        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            int width = Math.max(10 * 256, Math.min(sheet.getColumnWidth(column) + 768, 40 * 256));
            sheet.setColumnWidth(column, width);
        }
    }

    public record Styles(CellStyle text, CellStyle integer, CellStyle date, CellStyle dateTime) {
    }
}
