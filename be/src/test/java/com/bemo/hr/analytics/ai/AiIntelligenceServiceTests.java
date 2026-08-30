package com.bemo.hr.analytics.ai;

import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiIntelligenceServiceTests {

    @Mock
    private CustomerInvoiceRepository customerInvoiceRepository;
    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private BusinessPartyRepository businessPartyRepository;

    @InjectMocks
    private AiIntelligenceService aiService;

    @Test
    void shouldGenerateCashFlowForecastWithWideningBands_ACP1() {
        AiIntelligenceApi.CashFlowForecastResponse res = aiService.getCashFlowForecast(3);

        assertThat(res.forecastMonths()).isEqualTo(3);
        assertThat(res.points()).hasSize(3);

        AiIntelligenceApi.CashFlowPoint m1 = res.points().get(0);
        AiIntelligenceApi.CashFlowPoint m3 = res.points().get(2);

        BigDecimal spread1 = m1.upperBand().subtract(m1.lowerBand());
        BigDecimal spread3 = m3.upperBand().subtract(m3.lowerBand());

        assertThat(spread3).isGreaterThan(spread1);
    }

    @Test
    void shouldDetectExpenseAnomaliesWhenAbove2_5Sigma_ACP2() {
        SupplierInvoice inv1 = mock(SupplierInvoice.class);
        when(inv1.getSupplierId()).thenReturn("SUP-1");
        when(inv1.getTotalAmount()).thenReturn(new BigDecimal("1000.00"));

        SupplierInvoice inv2 = mock(SupplierInvoice.class);
        when(inv2.getSupplierId()).thenReturn("SUP-1");
        when(inv2.getTotalAmount()).thenReturn(new BigDecimal("1010.00"));

        SupplierInvoice inv3 = mock(SupplierInvoice.class);
        when(inv3.getSupplierId()).thenReturn("SUP-1");
        when(inv3.getTotalAmount()).thenReturn(new BigDecimal("990.00"));

        SupplierInvoice inv4 = mock(SupplierInvoice.class);
        when(inv4.getSupplierId()).thenReturn("SUP-1");
        when(inv4.getTotalAmount()).thenReturn(new BigDecimal("1005.00"));

        SupplierInvoice inv5 = mock(SupplierInvoice.class);
        when(inv5.getSupplierId()).thenReturn("SUP-1");
        when(inv5.getTotalAmount()).thenReturn(new BigDecimal("995.00"));

        SupplierInvoice inv6 = mock(SupplierInvoice.class);
        when(inv6.getSupplierId()).thenReturn("SUP-1");
        when(inv6.getTotalAmount()).thenReturn(new BigDecimal("1000.00"));

        SupplierInvoice invAnomaly = mock(SupplierInvoice.class);
        when(invAnomaly.getSupplierId()).thenReturn("SUP-1");
        when(invAnomaly.getTotalAmount()).thenReturn(new BigDecimal("10000.00"));

        when(supplierInvoiceRepository.findAll()).thenReturn(List.of(inv1, inv2, inv3, inv4, inv5, inv6, invAnomaly));

        List<AiIntelligenceApi.ExpenseAnomalyDto> anomalies = aiService.detectExpenseAnomalies();

        assertThat(anomalies).isNotEmpty();
        assertThat(anomalies.get(0).vendorId()).isEqualTo("SUP-1");
        assertThat(anomalies.get(0).currentAmount()).isEqualByComparingTo("10000.00");
        assertThat(anomalies.get(0).zScore().doubleValue()).isGreaterThan(2.5);
    }


    @Test
    void shouldClassifyCollectionsRiskScoreBands_ACP3() {
        BusinessParty goodCustomer = mock(BusinessParty.class);
        when(goodCustomer.getId()).thenReturn("CUST-1");
        when(goodCustomer.getName()).thenReturn("Fast Payer Co.");
        when(goodCustomer.getPartyType()).thenReturn("CUSTOMER");
        when(goodCustomer.getRiskLevel()).thenReturn("LOW");

        BusinessParty slowCustomer = mock(BusinessParty.class);
        when(slowCustomer.getId()).thenReturn("CUST-2");
        when(slowCustomer.getName()).thenReturn("Delayed Payments Ltd.");
        when(slowCustomer.getPartyType()).thenReturn("CUSTOMER");
        when(slowCustomer.getRiskLevel()).thenReturn("MEDIUM");

        when(businessPartyRepository.findAll()).thenReturn(List.of(goodCustomer, slowCustomer));

        List<AiIntelligenceApi.CollectionsRiskDto> risks = aiService.getCollectionsRisk();

        assertThat(risks).hasSize(2);
        assertThat(risks.get(0).riskBand()).isEqualTo("A");
        assertThat(risks.get(1).riskBand()).isEqualTo("B");
        assertThat(risks.get(0).scoringFactors()).isNotEmpty();
    }

    @Test
    void shouldExecuteNlQueryAndMapToWhitelistedDataset_ACP4() {
        AiIntelligenceApi.NlQueryResponse res = aiService.executeNlQuery(
                new AiIntelligenceApi.NlQueryRequest("ما هو إجمالي المبيعات المؤكدة هذا العام؟", null)
        );

        assertThat(res.success()).isTrue();
        assertThat(res.targetDataset()).isEqualTo("SALES_REVENUE");
        assertThat(res.appliedFilters()).contains("status=CONFIRMED");
        assertThat(res.summaryAnswer()).contains("285,400.00");
    }

    @Test
    void shouldVerifyZeroDatabaseMutatingWrites_ACP5() {
        aiService.getCashFlowForecast(6);
        aiService.detectExpenseAnomalies();
        aiService.getDemandForecast();
        aiService.getCollectionsRisk();

        verify(supplierInvoiceRepository, never()).save(any());
        verify(customerInvoiceRepository, never()).save(any());
        verify(inventoryItemRepository, never()).save(any());
        verify(businessPartyRepository, never()).save(any());
    }
}
