package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.api.SalesTargetApi;
import com.bemo.hr.trade.sales.domain.CommissionRule;
import com.bemo.hr.trade.sales.domain.SalesTarget;
import com.bemo.hr.trade.sales.infrastructure.CommissionRuleRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesTargetRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesTargetServiceTests {

    @Mock private SalesTargetRepository targetRepository;
    @Mock private CommissionRuleRepository ruleRepository;
    @Mock private CustomerInvoiceRepository invoiceRepository;
    @Mock private CustomerReceiptRepository receiptRepository;
    @InjectMocks private SalesTargetService service;

    private static final String APP_ID = "test-app";

    @Test
    void createTarget_success() {
        when(targetRepository.existsByScopeAndTargetRefIdAndPeriod(any(), anyString(), anyString())).thenReturn(false);
        when(targetRepository.save(any(SalesTarget.class))).thenAnswer(inv -> {
            SalesTarget t = inv.getArgument(0);
            return t;
        });

        SalesTargetApi.TargetResponse resp = service.createTarget(
                new SalesTargetApi.TargetRequest("REP", "emp-1", "2026-08", "REVENUE", new BigDecimal("100000")),
                APP_ID);

        assertEquals("REP", resp.scope());
        assertEquals(new BigDecimal("100000"), resp.targetValue());
        verify(targetRepository).save(any(SalesTarget.class));
    }

    @Test
    void createTarget_duplicate_throws() {
        when(targetRepository.existsByScopeAndTargetRefIdAndPeriod(any(), anyString(), anyString())).thenReturn(true);

        assertThrows(BusinessRuleException.class, () ->
                service.createTarget(
                        new SalesTargetApi.TargetRequest("REP", "emp-1", "2026-08", "REVENUE", new BigDecimal("100000")),
                        APP_ID));
    }

    @Test
    void createRule_success() {
        when(ruleRepository.existsByNameIgnoreCaseAndActiveTrue(anyString())).thenReturn(false);
        when(ruleRepository.save(any(CommissionRule.class))).thenAnswer(inv -> inv.getArgument(0));

        SalesTargetApi.CommissionRuleResponse resp = service.createRule(
                new SalesTargetApi.CommissionRuleRequest("Standard 5%", "INVOICE_TOTAL",
                        new BigDecimal("5.00"), BigDecimal.ZERO, true, null, null),
                APP_ID);

        assertEquals("Standard 5%", resp.name());
        assertEquals("INVOICE_TOTAL", resp.basis());
        assertEquals(new BigDecimal("5.00"), resp.percent());
        verify(ruleRepository).save(any(CommissionRule.class));
    }

    @Test
    void createRule_duplicate_name_throws() {
        when(ruleRepository.existsByNameIgnoreCaseAndActiveTrue("Standard 5%")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () ->
                service.createRule(
                        new SalesTargetApi.CommissionRuleRequest("Standard 5%", "INVOICE_TOTAL",
                                new BigDecimal("5.00"), BigDecimal.ZERO, true, null, null),
                        APP_ID));
    }

    @Test
    void computeStatement_no_rules_returns_zero() {
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of());

        SalesTargetApi.CommissionStatementResponse resp = service.computeStatement("emp-1", "2026-08");

        assertEquals("emp-1", resp.repId());
        assertEquals("2026-08", resp.period());
        assertTrue(resp.entries().isEmpty());
        assertEquals(BigDecimal.ZERO, resp.totalCommission());
    }

    @Test
    void computeStatement_below_min_amount_skipped() {
        CommissionRule rule = new CommissionRule("r1", APP_ID, "High Min", CommissionRule.Basis.INVOICE_TOTAL,
                new BigDecimal("5.00"), new BigDecimal("50000"), true, null, null);
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc()).thenReturn(List.of());

        SalesTargetApi.CommissionStatementResponse resp = service.computeStatement("emp-1", "2026-08");

        assertTrue(resp.entries().isEmpty());
        assertEquals(BigDecimal.ZERO, resp.totalCommission());
    }

    @Test
    void deleteTarget_calls_repo() {
        service.deleteTarget("t1");
        verify(targetRepository).deleteById("t1");
    }

    @Test
    void deleteRule_calls_repo() {
        service.deleteRule("r1");
        verify(ruleRepository).deleteById("r1");
    }
}
