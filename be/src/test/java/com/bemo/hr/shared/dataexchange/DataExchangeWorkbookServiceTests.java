package com.bemo.hr.shared.dataexchange;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DataExchangeWorkbookServiceTests {
    private final SpreadsheetTemplateCatalog catalog = new SpreadsheetTemplateCatalog();
    private final DataExchangeWorkbookService service = new DataExchangeWorkbookService(catalog);

    @Test
    void catalogContainsCrossModuleTemplates() {
        assertThat(service.catalog()).hasSizeGreaterThanOrEqualTo(25);
        assertThat(service.catalog()).extracting(DataExchangeWorkbookService.TemplateSummary::key)
                .contains("bulk-cash-advance", "manual-punch-log", "employee-onboarding", "payroll-variables",
                        "journal-entry", "bank-statement", "purchase-requisition", "sales-order", "physical-count",
                        "bom", "party-master", "translation-dictionary");
    }

    @Test
    void generatedTemplateContainsHeadersHintsAndSample() throws Exception {
        byte[] bytes = service.createTemplate("bank-statement", true);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("Date");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).startsWith("Required |");
            assertThat(workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue()).isNotBlank();
        }
    }

    @Test
    void validatorFlagsDuplicateUniqueIdentity() {
        byte[] bytes = service.createTemplate("worker-master", true);
        // Duplicate the generated sample row by editing the workbook in-memory.
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes));
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.getSheetAt(0);
            var source = sheet.getRow(2);
            var duplicate = sheet.createRow(3);
            for (int i = 0; i < source.getLastCellNum(); i++) {
                duplicate.createCell(i).setCellValue(source.getCell(i).toString());
            }
            workbook.write(out);
            var upload = new MockMultipartFile("file", "workers.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
            var result = service.validate("worker-master", upload);
            assertThat(result.valid()).isFalse();
            assertThat(result.invalidRows()).isGreaterThan(0);
            assertThat(result.sheets().get(0).rows().stream().flatMap(row -> row.errors().stream())
                    .anyMatch(error -> error.message().contains("Duplicate"))).isTrue();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
