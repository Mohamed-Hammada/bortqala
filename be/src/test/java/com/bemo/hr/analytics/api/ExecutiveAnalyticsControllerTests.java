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
}
