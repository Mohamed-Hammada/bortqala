package com.bemo.hr.finance.application.close;

import com.bemo.hr.operations.application.InventoryCloseProvider;
import com.bemo.hr.payroll.application.PayrollCloseProvider;
import com.bemo.hr.trade.procurement.application.ProcurementCloseProvider;
import com.bemo.hr.trade.sales.application.SalesCloseProvider;
import com.bemo.hr.workforce.application.WorkforceCloseProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinancialModuleCloseProvidersTests {

    @Test
    void everyInScopeFinancialModuleReportsItsUnfinishedDocuments() {
        CloseBlockerQueryService queries = mock(CloseBlockerQueryService.class);
        when(queries.dated(anyString(), anyString(), anyString(), anyString())).thenReturn(1L);
        when(queries.timestamped(anyString(), anyString(), anyString(), anyString())).thenReturn(1L);

        List<ModuleCloseProvider> providers = List.of(
                new PayrollCloseProvider(queries),
                new WorkforceCloseProvider(queries),
                new SalesCloseProvider(queries),
                new ProcurementCloseProvider(queries),
                new InventoryCloseProvider(queries),
                new TreasuryCloseProvider(queries));

        assertThat(providers).extracting(ModuleCloseProvider::getModuleName)
                .containsExactly("PAYROLL", "WORKFORCE", "SALES", "PROCUREMENT", "INVENTORY", "TREASURY");
        assertThat(providers).allSatisfy(provider -> {
            assertThat(provider.isPeriodCloseReady("period-1")).isFalse();
            assertThat(provider.getBlockerReason("period-1")).isPresent();
        });
    }
}
