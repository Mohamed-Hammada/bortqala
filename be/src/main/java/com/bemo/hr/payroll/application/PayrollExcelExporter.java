package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.api.PayrollApi;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class PayrollExcelExporter {

    public byte[] export(PayrollApi.SheetResponse sheet, ExcelExportOptions options) {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            boolean isArabic = options.locale().startsWith("ar");
            var sheetObj = workbook.createSheet(isArabic ? "كشف المرتبات والقبض" : "Payroll Register");
            sheetObj.setRightToLeft(isArabic);

            // Header Style
            var headerStyle = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create title
            var titleRow = sheetObj.createRow(0);
            var titleCell = titleRow.createCell(0);
            titleCell.setCellValue((isArabic ? "كشف صرف المرتبات والقبض — " : "Payroll Disbursal Register — ")
                    + sheet.periodYear() + "/" + String.format("%02d", sheet.periodMonth()));

            // Summary metrics row
            var summaryRow = sheetObj.createRow(2);
            summaryRow.createCell(0).setCellValue(isArabic ? "إجمالي الموظفين:" : "Total Employees:");
            summaryRow.createCell(1).setCellValue(sheet.summary().totalEmployees());
            summaryRow.createCell(2).setCellValue(isArabic ? "تم الصرف:" : "Paid:");
            summaryRow.createCell(3).setCellValue(sheet.summary().paidCount());
            summaryRow.createCell(4).setCellValue(isArabic ? "إجمالي المدفوع:" : "Total Paid:");
            summaryRow.createCell(5).setCellValue(sheet.summary().totalPaidAmount().doubleValue());

            // Column Headers
            var headers = isArabic ? new String[]{
                    "كود الموظف", "اسم الموظف", "الفئة", "نوع التوظيف", "الفترة", "إجمالي المستحق",
                    "السلف المخصومة", "خصومات أخرى", "المكافآت", "الصافي المستحق", "حالة القبض", "طريقة الصرف", "تاريخ الصرف", "ملاحظات"
            } : new String[]{
                    "Code", "Name", "Category", "Employment Type", "Period", "Gross Amount",
                    "Advances Deducted", "Other Deductions", "Bonuses", "Net Amount", "Status", "Payment Method", "Paid At", "Notes"
            };

            var headRow = sheetObj.createRow(4);
            for (int i = 0; i < headers.length; i++) {
                var c = headRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIndex = 5;
            for (var row : sheet.rows()) {
                var r = sheetObj.createRow(rowIndex++);
                r.createCell(0).setCellValue(safe(row.employeeCode()));
                r.createCell(1).setCellValue(safe(row.employeeName()));
                r.createCell(2).setCellValue(safe(row.categoryName()));
                r.createCell(3).setCellValue(safe(row.employmentType()));
                r.createCell(4).setCellValue(safe(row.periodStart() + " -> " + row.periodEnd()));
                r.createCell(5).setCellValue(row.grossAmount().doubleValue());
                r.createCell(6).setCellValue(row.advancesDeducted().doubleValue());
                r.createCell(7).setCellValue(row.otherDeductions().doubleValue());
                r.createCell(8).setCellValue(row.bonuses().doubleValue());
                r.createCell(9).setCellValue(row.netAmount().doubleValue());
                r.createCell(10).setCellValue(row.paymentStatus().name());
                r.createCell(11).setCellValue(row.paymentMethod() == null ? "—" : row.paymentMethod().name());
                r.createCell(12).setCellValue(row.paidAt() == null ? "—" : row.paidAt().toString());
                r.createCell(13).setCellValue(safe(row.note()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheetObj.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Payroll Excel export", e);
        }
    }

    private static String safe(String value) {
        return com.bemo.hr.reporting.infrastructure.ExcelExportSupport.escapeFormula(value);
    }
}
