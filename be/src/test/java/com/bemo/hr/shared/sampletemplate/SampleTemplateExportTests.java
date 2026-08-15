package com.bemo.hr.shared.sampletemplate;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SampleTemplateExportTests {

    private SampleTemplateCatalog catalog;
    private SampleTemplateWorkbookService workbookService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        catalog = new SampleTemplateCatalog();
        workbookService = new SampleTemplateWorkbookService();
        SampleTemplateController controller = new SampleTemplateController(catalog, workbookService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ATTENDANCE", "EMPLOYEE_MASTER", "CHART_OF_ACCOUNTS", "BUSINESS_PARTIES",
        "INVENTORY_ITEMS", "BOM_MASTER", "WORKERS", "WORKFORCE_ATTENDANCE",
        "BANK_STATEMENT", "TRANSLATIONS", "SUPPLIER_DOCUMENTS"
    })
    void catalogProvidesValidTemplateAndGeneratesWorkbook(String key) throws IOException {
        SampleTemplateCatalog.Template template = catalog.get(key);
        assertThat(template).isNotNull();
        assertThat(template.fileName()).isNotBlank().endsWith(".xlsx");
        assertThat(template.columns()).isNotEmpty();

        byte[] bytes = workbookService.create(template);
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Template - النموذج");
            assertThat(workbook.getSheetName(1)).isEqualTo("Instructions - تعليمات");

            var templateSheet = workbook.getSheetAt(0);
            assertThat((Object) templateSheet.getRow(0)).isNotNull();
            assertThat(templateSheet.getRow(0).getPhysicalNumberOfCells()).isEqualTo(template.columns().size());

            var instructionsSheet = workbook.getSheetAt(1);
            assertThat((Object) instructionsSheet.getRow(0)).isNotNull();
            assertThat(instructionsSheet.getPhysicalNumberOfRows()).isEqualTo(template.columns().size() + 1);
        }
    }

    @Test
    void catalogThrowsOnUnknownKey() {
        assertThatThrownBy(() -> catalog.get("UNKNOWN_KEY"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported sample template: UNKNOWN_KEY");
    }

    @Test
    void attendanceEndpointReturnsXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/imports/sample-template?format=xlsx"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("biometric-attendance-sample.xlsx")));
    }

    @Test
    void smartImportEndpointReturnsXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/smart-import/EMPLOYEE_MASTER/sample-template"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("employee-master-sample.xlsx")));
    }

    @Test
    void workforceEndpointsReturnXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/workforce/imports/sample-template?type=WORKERS"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("contractor-workers-sample.xlsx")));

        mockMvc.perform(get("/api/v1/workforce/imports/sample-template?type=ATTENDANCE"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("workforce-attendance-sample.xlsx")));
    }

    @Test
    void bankEndpointReturnsXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/finance/bank-reconciliation/sample-template"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("bank-statement-sample.xlsx")));
    }

    @Test
    void translationsEndpointReturnsXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/i18n/admin/translations/sample-template"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("translations-sample.xlsx")));
    }

    @Test
    void supplierDocumentsEndpointReturnsXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/parties/documents/sample-template"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("supplier-document-requirements.xlsx")));
    }
}
