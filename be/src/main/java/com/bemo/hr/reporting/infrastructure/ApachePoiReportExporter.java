package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.application.ReportExporter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ApachePoiReportExporter implements ReportExporter {
    @Override
    public byte[] export(ReportingApi.Details details) {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var headerFont = workbook.createFont(); headerFont.setBold(true); headerStyle.setFont(headerFont);

            var summary = workbook.createSheet("Summary");
            summary.setRightToLeft(true);
            String[][] facts = {
                    {"Report ID", details.report().id()}, {"Period", details.report().periodStart() + " — " + details.report().periodEnd()},
                    {"Status", details.report().status().name()}, {"Unresolved", String.valueOf(details.report().unresolvedCount())},
                    {"Generated", java.time.Instant.now().toString()}, {"Report version", String.valueOf(details.report().version())}
            };
            for (int index = 0; index < facts.length; index++) {
                var row = summary.createRow(index); row.createCell(0).setCellValue(facts[index][0]); row.createCell(1).setCellValue(facts[index][1]);
            }
            summary.autoSizeColumn(0); summary.autoSizeColumn(1);

            var sheet = workbook.createSheet("Daily attendance"); sheet.setRightToLeft(true); sheet.createFreezePane(0, 1);
            String[] headers = {"Date", "Employee code", "Employee", "Category", "First punch", "Last punch", "Punches",
                    "Expected min", "Worked min", "Effective min", "Late min", "Early min", "Overtime min", "Status", "Decision", "Note"};
            var header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) { var cell = header.createCell(index); cell.setCellValue(headers[index]); cell.setCellStyle(headerStyle); }
            int rowIndex = 1;
            for (var item : details.dailyResults()) {
                var row = sheet.createRow(rowIndex++); int cell = 0;
                row.createCell(cell++).setCellValue(item.workDate().toString()); row.createCell(cell++).setCellValue(item.employeeCode());
                row.createCell(cell++).setCellValue(item.employeeName()); row.createCell(cell++).setCellValue(item.categoryName());
                row.createCell(cell++).setCellValue(item.firstPunch() == null ? "" : item.firstPunch().toString());
                row.createCell(cell++).setCellValue(item.lastPunch() == null ? "" : item.lastPunch().toString());
                row.createCell(cell++).setCellValue(item.punchCount()); row.createCell(cell++).setCellValue(item.expectedMinutes());
                row.createCell(cell++).setCellValue(item.workedMinutes()); row.createCell(cell++).setCellValue(item.effectiveWorkedMinutes());
                row.createCell(cell++).setCellValue(item.lateMinutes()); row.createCell(cell++).setCellValue(item.earlyLeaveMinutes());
                row.createCell(cell++).setCellValue(item.overtimeMinutes()); row.createCell(cell++).setCellValue(item.status().name());
                row.createCell(cell++).setCellValue(item.decision() == null ? "" : item.decision().name());
                row.createCell(cell).setCellValue(item.decisionNote() == null ? "" : item.decisionNote());
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, headers.length - 1));
            for (int index = 0; index < headers.length; index++) { sheet.autoSizeColumn(index); sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 12_000)); }
            workbook.write(output); return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate Excel report.", exception);
        }
    }
}
