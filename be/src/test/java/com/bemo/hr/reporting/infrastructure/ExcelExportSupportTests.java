package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.security.ExcelTableStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportSupportTests {
    @Test
    void createsANativeRightToLeftExcelTableWithTheSelectedStyle() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var options = new ExcelExportOptions("ar-EG", ExcelTableStyle.GREEN);
            var sheet = ExcelExportSupport.sheet(workbook, "الموظفون", options.rightToLeft());
            ExcelExportSupport.writeHeader(sheet, List.of("الكود", "الاسم"));
            ExcelExportSupport.writeRow(sheet, 1, List.of("EMP-001", "أحمد"), ExcelExportSupport.styles(workbook));

            ExcelExportSupport.finishTable(sheet, 1, 2, "EmployeesTable", options);

            assertThat(sheet.isRightToLeft()).isTrue();
            assertThat(sheet.getTables()).hasSize(1);
            assertThat(sheet.getTables().getFirst().getName()).isEqualTo("EmployeesTable");
            assertThat(sheet.getTables().getFirst().getStyle().getName()).isEqualTo("TableStyleMedium4");
        }
    }
}
