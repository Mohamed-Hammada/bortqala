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
            assertThat(sheet.getTables().get(0).getName()).isEqualTo("EmployeesTable");
            assertThat(sheet.getTables().get(0).getStyle().getName()).isEqualTo("TableStyleMedium4");
        }
    }

    @Test
    void escapesFormulaInjectionPrefixesOnUserControlledText() {
        assertThat(ExcelExportSupport.escapeFormula("=SUM(A1:A9)")).isEqualTo("'=SUM(A1:A9)");
        assertThat(ExcelExportSupport.escapeFormula("+1+2")).isEqualTo("'+1+2");
        assertThat(ExcelExportSupport.escapeFormula("-5")).isEqualTo("'-5");
        assertThat(ExcelExportSupport.escapeFormula("@cmd")).isEqualTo("'@cmd");
    }

    @Test
    void leavesSafeTextAndNumbersUnchanged() {
        assertThat(ExcelExportSupport.escapeFormula("EMP-001")).isEqualTo("EMP-001");
        assertThat(ExcelExportSupport.escapeFormula("أحمد علي")).isEqualTo("أحمد علي");
        assertThat(ExcelExportSupport.escapeFormula("(123)")).isEqualTo("(123)");
        assertThat(ExcelExportSupport.escapeFormula("")).isEmpty();
        assertThat(ExcelExportSupport.escapeFormula(null)).isNull();
        assertThat(ExcelExportSupport.escapeFormula("5.5")).isEqualTo("5.5");
    }

    @Test
    void writeRowEscapesDangerousLeadingCharacters() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = ExcelExportSupport.sheet(workbook, "Sheet", false);
            ExcelExportSupport.writeHeader(sheet, List.of("قيمة"));
            ExcelExportSupport.writeRow(sheet, 1, List.of("=1+1"), ExcelExportSupport.styles(workbook));

            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("'=1+1");
        }
    }
}
