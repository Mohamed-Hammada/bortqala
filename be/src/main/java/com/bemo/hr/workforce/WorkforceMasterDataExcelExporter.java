package com.bemo.hr.workforce;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class WorkforceMasterDataExcelExporter {
    public byte[] contractors(List<WorkforceApi.ContractorResponse> values) {
        return workbook("سجل المقاولين", "ContractorsTable",
                new String[]{"الكود", "الاسم", "الاسم التجاري", "الهاتف", "الرقم الضريبي", "نموذج المحاسبة", "دورة التسوية", "اليومية الافتراضية", "الحالة"},
                values.stream().map(value -> new Object[]{value.code(), value.name(), value.tradeName(), value.phone(), value.taxId(), value.accountingModel(), value.settlementCycleDays(), value.defaultDailyRate(), value.status()}).toList());
    }

    public byte[] workers(List<WorkforceApi.WorkerResponse> values) {
        return workbook("سجل العمال", "WorkersTable",
                new String[]{"الكود", "الاسم الكامل", "المقاول", "الفئة", "اليومية", "ساعات اليوم", "طريقة الحضور", "الهاتف", "الرقم القومي", "الحالة"},
                values.stream().map(value -> new Object[]{value.code(), value.fullName(), value.contractorName(), value.categoryName(), value.defaultDailyRate(), value.standardDailyHours(), value.attendanceMode(), value.phone(), value.nationalId(), value.status()}).toList());
    }

    public byte[] categories(List<WorkforceApi.CategoryResponse> values) {
        return workbook("تصنيفات العمال", "WorkerCategoriesTable",
                new String[]{"الكود", "اسم الفئة", "الوصف", "اليومية الافتراضية", "ساعات اليوم", "دورة التسوية", "الحالة"},
                values.stream().map(value -> new Object[]{value.code(), value.name(), value.description(), value.defaultDailyRate(), value.standardDailyHours(), value.defaultSettlementCycle(), value.status()}).toList());
    }

    private byte[] workbook(String sheetName, String tableName, String[] headings, List<Object[]> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            sheet.setRightToLeft(true);
            sheet.createFreezePane(0, 1);
            CellStyle header = workbook.createCellStyle();
            header.setAlignment(HorizontalAlignment.CENTER);
            var font = workbook.createFont();
            font.setBold(true);
            header.setFont(font);
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < headings.length; column++) {
                var cell = headerRow.createCell(column);
                cell.setCellValue(headings[column]);
                cell.setCellStyle(header);
            }
            int rowIndex = 1;
            for (Object[] values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int column = 0; column < values.length; column++) write(row, column, values[column]);
            }
            var tableArea = new org.apache.poi.ss.util.AreaReference(
                    new org.apache.poi.ss.util.CellReference(0, 0),
                    new org.apache.poi.ss.util.CellReference(Math.max(1, rows.size()), headings.length - 1),
                    org.apache.poi.ss.SpreadsheetVersion.EXCEL2007);
            var table = ((org.apache.poi.xssf.usermodel.XSSFSheet) sheet).createTable(tableArea);
            table.setName(tableName);
            table.setDisplayName(tableName);
            table.getCTTable().addNewTableStyleInfo().setName("TableStyleMedium2");
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rows.size()), 0, headings.length - 1));
            for (int column = 0; column < headings.length; column++) sheet.autoSizeColumn(column);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("تعذّر إنشاء ملف Excel لبيانات القوى العاملة.", exception);
        }
    }

    private void write(Row row, int column, Object value) {
        var cell = row.createCell(column);
        if (value instanceof Number number) cell.setCellValue(number.doubleValue());
        else
            cell.setCellValue(com.bemo.hr.reporting.infrastructure.ExcelExportSupport.escapeFormula(value == null ? "" : value.toString()));
    }
}
