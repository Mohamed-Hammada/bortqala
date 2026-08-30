package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.SubledgerReconciliationService;
import com.bemo.hr.finance.application.close.SubledgerReconciliationProvider;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReconciliationCenterControllerTests {

    private SubledgerReconciliationService service;
    private FiscalPeriodRepository periodRepository;
    private SubledgerReconciliationProvider inventoryProvider;
    private ReconciliationCenterController controller;

    @BeforeEach
    void setUp() {
        service = mock(SubledgerReconciliationService.class);
        periodRepository = mock(FiscalPeriodRepository.class);
        inventoryProvider = mock(SubledgerReconciliationProvider.class);

        when(inventoryProvider.type()).thenReturn(SubledgerReconciliationReport.SubledgerType.INVENTORY);

        FiscalPeriod period = new FiscalPeriod(2026, 8, "2026-08", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), FiscalPeriod.Status.OPEN);
        when(periodRepository.findAllByOrderByFiscalYearDescPeriodNumberAsc()).thenReturn(List.of(period));
        when(periodRepository.findById("period-1")).thenReturn(Optional.of(period));

        controller = new ReconciliationCenterController(
                service,
                List.of(inventoryProvider),
                periodRepository,
                new ObjectMapper()
        );
    }

    @Test
    void returnsOverviewWithAllDomains() {
        when(inventoryProvider.calculate(eq("period-1"), any())).thenReturn(
                new SubledgerReconciliationProvider.ReconciliationCalculation(
                        SubledgerReconciliationReport.SubledgerType.INVENTORY,
                        new BigDecimal("250000.00"),
                        new BigDecimal("250000.00"),
                        BigDecimal.ZERO,
                        true,
                        List.of()
                )
        );

        ResponseEntity<List<ReconciliationCenterController.ReconciliationDomainSummary>> response =
                controller.getOverview("period-1");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();

        var invSummary = response.getBody().stream()
                .filter(s -> s.subledgerType().equals("INVENTORY"))
                .findFirst();
        assertThat(invSummary).isPresent();
        assertThat(invSummary.get().isBalanced()).isTrue();
        assertThat(invSummary.get().glBalance()).isEqualByComparingTo(new BigDecimal("250000.00"));
    }

    @Test
    void returnsDrilldownDiscrepancyItems() {
        when(inventoryProvider.calculate(eq("period-1"), any())).thenReturn(
                new SubledgerReconciliationProvider.ReconciliationCalculation(
                        SubledgerReconciliationReport.SubledgerType.INVENTORY,
                        new BigDecimal("250000.00"),
                        new BigDecimal("230000.00"),
                        new BigDecimal("20000.00"),
                        false,
                        List.of(new SubledgerReconciliationProvider.SourceDifference(
                                "doc-1", "GRN-2026-001", new BigDecimal("20000.00"), BigDecimal.ZERO
                        ))
                )
        );

        ResponseEntity<List<ReconciliationCenterController.DiscrepancyDetailItem>> response =
                controller.getDrilldown("INVENTORY", "period-1");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).documentNumber()).isEqualTo("GRN-2026-001");
        assertThat(response.getBody().get(0).discrepancyReason()).isEqualTo("UNPOSTED_SUBLEDGER_DOCUMENT");
    }
}
