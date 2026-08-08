package com.bemo.hr.workforce;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkforceMasterDataExcelExporterTest {
    @Test
    void exportsWorkerCategoriesAsAnArabicRtlWorkbookWithNumericAmounts() throws Exception {
        var category = new WorkforceApi.CategoryResponse("1", "WELD", "لحام", "عمال اللحام",
                new BigDecimal("275.50"), new BigDecimal("8"), "HALF_MONTH", "ACTIVE",
                "WORKER", true, 1L, 2L);

        byte[] content = new WorkforceMasterDataExcelExporter().categories(List.of(category));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var sheet = workbook.getSheet("تصنيفات العمال");
            assertThat(sheet).isNotNull();
            assertThat(sheet.isRightToLeft()).isTrue();
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("WELD");
            assertThat(sheet.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(275.5);
            assertThat(sheet.getTables()).hasSize(1);
        }
    }
}
