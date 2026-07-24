package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.api.ReportingApi;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.application.ReportExporter;
import com.bemo.hr.shared.i18n.TranslationService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApachePoiReportExporter implements ReportExporter {
    private final TranslationService translationService;

    @Override
    public byte[] export(ReportingApi.Details details, ExcelExportOptions options) {
        var messages = ExcelExportSupport.messages(translationService, options);
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var styles = ExcelExportSupport.styles(workbook);
            var summary = ExcelExportSupport.sheet(workbook,
                    ExcelExportSupport.text(messages, "export.sheet.summary"), options.rightToLeft());
            ExcelExportSupport.writeHeader(summary, List.of(
                    ExcelExportSupport.text(messages, "export.column.field"),
                    ExcelExportSupport.text(messages, "export.column.value")));
            List<List<?>> facts = List.of(
                    List.of(ExcelExportSupport.text(messages, "export.field.reportId"), details.report().id()),
                    List.of(ExcelExportSupport.text(messages, "export.field.period"),
                            details.report().periodStart() + " — " + details.report().periodEnd()),
                    List.of(ExcelExportSupport.text(messages, "export.field.payCycle"),
                            ExcelExportSupport.enumText(messages, details.report().payCycle())),
                    List.of(ExcelExportSupport.text(messages, "export.field.status"),
                            ExcelExportSupport.enumText(messages, details.report().status())),
                    List.of(ExcelExportSupport.text(messages, "export.field.unresolved"), details.report().unresolvedCount()),
                    List.of(ExcelExportSupport.text(messages, "export.field.generated"), Instant.now()),
                    List.of(ExcelExportSupport.text(messages, "export.field.reportVersion"), details.report().version()));
            for (int index = 0; index < facts.size(); index++) {
                ExcelExportSupport.writeRow(summary, index + 1, facts.get(index), styles);
            }
            ExcelExportSupport.finishTable(summary, facts.size(), 2, "ReportSummaryTable", options);

            var attendance = ExcelExportSupport.sheet(workbook,
                    ExcelExportSupport.text(messages, "export.sheet.dailyAttendance"), options.rightToLeft());
            var headerKeys = List.of("date", "employeeCode", "employee", "category", "firstPunch", "lastPunch",
                    "punches", "expectedMinutes", "workedMinutes", "effectiveMinutes", "lateMinutes", "earlyMinutes",
                    "overtimeMinutes", "status", "decision", "note");
            ExcelExportSupport.writeHeader(attendance, headerKeys.stream()
                    .map(key -> ExcelExportSupport.text(messages, "export.column." + key)).toList());
            int rowIndex = 1;
            for (var item : details.dailyResults()) {
                ExcelExportSupport.writeRow(attendance, rowIndex++, List.of(
                        item.workDate(), item.employeeCode(), item.employeeName(), item.categoryName(),
                        item.firstPunch() == null ? "" : item.firstPunch(), item.lastPunch() == null ? "" : item.lastPunch(),
                        item.punchCount(), item.expectedMinutes(), item.workedMinutes(), item.effectiveWorkedMinutes(),
                        item.lateMinutes(), item.earlyLeaveMinutes(), item.overtimeMinutes(),
                        ExcelExportSupport.enumText(messages, item.status()),
                        ExcelExportSupport.enumText(messages, item.decision()),
                        item.decisionNote() == null ? "" : item.decisionNote()), styles);
            }
            ExcelExportSupport.finishTable(attendance, rowIndex - 1, headerKeys.size(), "DailyAttendanceTable", options);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate Excel report.", exception);
        }
    }
}
