package com.bemo.hr.analytics.api;

import com.bemo.hr.analytics.api.ExecutiveAnalyticsApi.*;
import com.bemo.hr.analytics.application.ExecutiveAnalyticsService;
import com.bemo.hr.analytics.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutiveAnalyticsControllerTests {

    @Mock
    private ExecutiveAnalyticsService analyticsService;

    private ExecutiveAnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new ExecutiveAnalyticsController(analyticsService);
    }

    @Test
    void getKpiRegistryDelegatesToService() {
        when(analyticsService.getKpiRegistry()).thenReturn(List.of(
                new KpiDefinitionResponse("NET_PROFIT_MARGIN", "Net Profit Margin", "هامش صافي الربح", KpiCategory.FINANCIAL, KpiGrain.MONTHLY, KpiUnit.PERCENT, "Formula", "معادلة", "Finance", "P_FINANCE_READ")
        ));

        var result = controller.getKpiRegistry();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).key()).isEqualTo("NET_PROFIT_MARGIN");
        verify(analyticsService).getKpiRegistry();
    }

    @Test
    void getOverviewDelegatesToService() {
        ExecutiveOverviewResponse mockOverview = new ExecutiveOverviewResponse(
                "2026-08",
                123456789L,
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(700_000),
                BigDecimal.valueOf(300_000),
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(250_000),
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(50_000),
                BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(2_000_000),
                BigDecimal.valueOf(100_000),
                45,
                BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(98.0),
                BigDecimal.valueOf(100.0),
                List.of()
        );
        when(analyticsService.getExecutiveOverview(any(), any(), any(), any())).thenReturn(mockOverview);

        var result = controller.getExecutiveOverview("2026-08", null, null, null);

        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        assertThat(result.grossProfit()).isEqualByComparingTo(BigDecimal.valueOf(300_000));
        verify(analyticsService).getExecutiveOverview("2026-08", null, null, null);
    }

    @Test
    void recordSnapshotDelegatesToService() {
        CreateSnapshotPayload payload = new CreateSnapshotPayload(
                "2026-Q3",
                KpiCategory.FINANCIAL,
                "NET_PROFIT_MARGIN",
                BigDecimal.valueOf(25.0),
                BigDecimal.valueOf(28.5),
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(14.0),
                TrendDirection.UP,
                ReconciliationStatus.RECONCILED,
                "/finance/accounts",
                "{}"
        );
        ExecutiveKpiSnapshotResponse mockResponse = new ExecutiveKpiSnapshotResponse(
                "id-1",
                12345L,
                "2026-Q3",
                KpiCategory.FINANCIAL,
                "NET_PROFIT_MARGIN",
                BigDecimal.valueOf(25.0),
                BigDecimal.valueOf(28.5),
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(14.0),
                TrendDirection.UP,
                ReconciliationStatus.RECONCILED,
                "/finance/accounts",
                "{}",
                12345L
        );
        when(analyticsService.recordSnapshot(payload)).thenReturn(mockResponse);

        var result = controller.recordSnapshot(payload);

        assertThat(result.id()).isEqualTo("id-1");
        verify(analyticsService).recordSnapshot(payload);
    }

    @Test
    void getOwnerCockpitDelegatesToService() {
        OwnerCockpitResponse mockResponse = mock(OwnerCockpitResponse.class);
        when(analyticsService.getOwnerCockpit("2026-09", "comp-1", "branch-1")).thenReturn(mockResponse);

        var result = controller.getOwnerCockpit("2026-09", "comp-1", "branch-1");

        assertThat(result).isSameAs(mockResponse);
        verify(analyticsService).getOwnerCockpit("2026-09", "comp-1", "branch-1");
    }

    @Test
    void exportExecutiveCockpitReturnsExcelAttachment() {
        byte[] mockBytes = new byte[]{1, 2, 3};
        when(analyticsService.exportExecutiveCockpitExcel("2026-09", null, null)).thenReturn(mockBytes);

        var response = controller.exportExecutiveCockpit("2026-09", null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(mockBytes);
        assertThat(response.getHeaders().getContentType().toString()).contains("spreadsheetml.sheet");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("Executive_Cockpit_2026-09.xlsx");
        verify(analyticsService).exportExecutiveCockpitExcel("2026-09", null, null);
    }

    @Test
    void getTargetsDelegatesToService() {
        CockpitTargetResponse mockResponse = new CockpitTargetResponse(
                "tgt-1",
                "2026-Q3",
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(200_000),
                BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(100_000),
                "Q3 Targets",
                123456789L
        );
        when(analyticsService.getTargets("2026-Q3")).thenReturn(mockResponse);

        var result = controller.getTargets("2026-Q3");

        assertThat(result.periodKey()).isEqualTo("2026-Q3");
        assertThat(result.targetRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
        verify(analyticsService).getTargets("2026-Q3");
    }

    @Test
    void saveTargetsDelegatesToService() {
        SaveCockpitTargetRequest request = new SaveCockpitTargetRequest(
                "2026-Q3",
                BigDecimal.valueOf(1_500_000),
                BigDecimal.valueOf(35.0),
                BigDecimal.valueOf(250_000),
                BigDecimal.valueOf(600_000),
                BigDecimal.valueOf(80_000),
                "Updated Q3"
        );
        CockpitTargetResponse mockResponse = new CockpitTargetResponse(
                "tgt-1",
                "2026-Q3",
                BigDecimal.valueOf(1_500_000),
                BigDecimal.valueOf(35.0),
                BigDecimal.valueOf(250_000),
                BigDecimal.valueOf(600_000),
                BigDecimal.valueOf(80_000),
                "Updated Q3",
                123456789L
        );
        when(analyticsService.saveTargets(request)).thenReturn(mockResponse);

        var result = controller.saveTargets(request);

        assertThat(result.targetRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1_500_000));
        verify(analyticsService).saveTargets(request);
    }
}

