package com.bemo.hr.reportbuilder;

import com.bemo.hr.reportbuilder.application.ReportBuilderService;
import com.bemo.hr.reportbuilder.domain.SavedReport;
import com.bemo.hr.reportbuilder.domain.SavedReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportBuilderServiceTests {

    @Mock private SavedReportRepository savedReportRepo;
    @InjectMocks private ReportBuilderService service;

    @Test
    void listDatasets_returnsFourDatasets() {
        List<Map<String, String>> datasets = service.listDatasets();
        assertThat(datasets).hasSize(4);
        assertThat(datasets).extracting(d -> d.get("code"))
                .containsExactlyInAnyOrder("sales_lines", "attendance_days", "journal_lines", "stock_movements");
    }

    @Test
    void executeQuery_validatesFields() {
        assertThatThrownBy(() -> service.executeQuery("sales_lines",
                List.of("nonexistent"), List.of(), List.of(), List.of(), 10))
                .hasMessageContaining("nonexistent");
    }

    @Test
    void executeQuery_clampsLimitToMax() {
        Map<String, Object> result = service.executeQuery("sales_lines",
                List.of("branchName"), List.of("net"), List.of(), List.of(), 99999);
        assertThat((int) result.get("limit")).isEqualTo(10000);
    }

    @Test
    void executeQuery_returnsColumnsWithMetadata() {
        Map<String, Object> result = service.executeQuery("sales_lines",
                List.of("branchName"), List.of("net"), List.of(), List.of(), 10);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> columns = (List<Map<String, String>>) result.get("columns");
        assertThat(columns).hasSize(2);
        assertThat(columns.get(0).get("role")).isEqualTo("dimension");
        assertThat(columns.get(1).get("role")).isEqualTo("measure");
        assertThat(columns.get(1).get("aggregate")).isEqualTo("SUM");
    }

    @Test
    void saveReport_delegatesToRepository() {
        when(savedReportRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SavedReport r = service.saveReport("app1", "My Report", "sales_lines",
                "{\"dimensions\":[\"branchName\"]}", "user1");
        assertThat(r.getName()).isEqualTo("My Report");
        assertThat(r.getDatasetCode()).isEqualTo("sales_lines");
        verify(savedReportRepo).save(any());
    }
}
