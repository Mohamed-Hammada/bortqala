package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.trade.sales.api.SalesTargetApi;
import com.bemo.hr.trade.sales.domain.CommissionRule;
import com.bemo.hr.trade.sales.domain.SalesCommissionPayout;
import com.bemo.hr.trade.sales.domain.SalesTarget;
import com.bemo.hr.trade.sales.infrastructure.CommissionRuleRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesTargetRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesCommissionPayoutRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesTargetServiceTests {

    @Mock private SalesTargetRepository targetRepository;
    @Mock private CommissionRuleRepository ruleRepository;
    @Mock private CustomerInvoiceRepository invoiceRepository;
    @Mock private CustomerReceiptRepository receiptRepository;
    @Mock private SalesCommissionPayoutRepository payoutRepository;
    @Mock private TranslationService translationService;
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
        when(payoutRepository.findByRepIdAndPeriod("emp-1", "2026-08")).thenReturn(Optional.empty());

        SalesTargetApi.CommissionStatementResponse resp = service.computeStatement("emp-1", "2026-08");

        assertEquals("emp-1", resp.repId());
        assertEquals("2026-08", resp.period());
        assertTrue(resp.entries().isEmpty());
        assertEquals(BigDecimal.ZERO, resp.totalCommission());
        assertFalse(resp.payrollSent());
    }

    @Test
    void computeStatement_below_min_amount_skipped() {
        CommissionRule rule = new CommissionRule("r1", APP_ID, "High Min", CommissionRule.Basis.INVOICE_TOTAL,
                new BigDecimal("5.00"), new BigDecimal("50000"), true, null, null);
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc()).thenReturn(List.of());
        when(payoutRepository.findByRepIdAndPeriod("emp-1", "2026-08")).thenReturn(Optional.empty());

        SalesTargetApi.CommissionStatementResponse resp = service.computeStatement("emp-1", "2026-08");

        assertTrue(resp.entries().isEmpty());
        assertEquals(BigDecimal.ZERO, resp.totalCommission());
        assertFalse(resp.payrollSent());
    }

    @Test
    void computeStatement_marks_payroll_sent_when_payout_exists() {
        SalesCommissionPayout payout = new SalesCommissionPayout("p1", APP_ID, "emp-1", "2026-08",
                new BigDecimal("125.00"), "admin");
        when(payoutRepository.findByRepIdAndPeriod("emp-1", "2026-08")).thenReturn(Optional.of(payout));

        SalesTargetApi.CommissionStatementResponse resp = service.computeStatement("emp-1", "2026-08");

        assertTrue(resp.payrollSent());
        assertEquals(payout.getSentAt().toEpochMilli(), resp.payrollSentAt());
    }

    @Test
    void sendToPayroll_firstSend_creates_payout_and_reports_false() {
        SalesCommissionPayout saved = new SalesCommissionPayout("p1", APP_ID, "emp-1", "2026-08",
                new BigDecimal("100.00"), "admin");
        when(payoutRepository.findByRepIdAndPeriod("emp-1", "2026-08")).thenReturn(Optional.empty());
        when(payoutRepository.save(any(SalesCommissionPayout.class))).thenReturn(saved);

        SalesTargetApi.PayrollSendResponse resp = service.sendToPayroll("emp-1", "2026-08", APP_ID, "admin");

        assertFalse(resp.alreadySent());
        assertEquals(new BigDecimal("100.00"), resp.totalCommission());
        assertEquals(saved.getSentAt().toEpochMilli(), resp.sentAt());
        verify(payoutRepository).save(any(SalesCommissionPayout.class));
    }

    @Test
    void sendToPayroll_secondSend_is_idempotent_replay() {
        SalesCommissionPayout existing = new SalesCommissionPayout("p1", APP_ID, "emp-1", "2026-08",
                new BigDecimal("100.00"), "admin");
        when(payoutRepository.findByRepIdAndPeriod("emp-1", "2026-08")).thenReturn(Optional.of(existing));

        SalesTargetApi.PayrollSendResponse resp = service.sendToPayroll("emp-1", "2026-08", APP_ID, "admin");

        assertTrue(resp.alreadySent());
        assertEquals(new BigDecimal("100.00"), resp.totalCommission());
        verify(payoutRepository, never()).save(any(SalesCommissionPayout.class));
    }

    @Test
    void exportStatement_produces_workbook_bytes() {
        CommissionRule rule = new CommissionRule("r1", APP_ID, "Std 5%", CommissionRule.Basis.COLLECTED,
                new BigDecimal("5.00"), BigDecimal.ZERO, true, null, null);
        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(receiptRepository.findAllByOrderByReceiptDateDescCreatedAtDesc()).thenReturn(List.of());
        when(payoutRepository.findByRepIdAndPeriod("emp-1", "2026-08")).thenReturn(Optional.empty());
        when(translationService.bundle(anyString())).thenReturn(new TranslationService.TranslationBundle("en-US", APP_ID, java.util.Map.of(
                "export.sheet.commissions", "Commission statements",
                "export.column.rule", "Rule",
                "export.column.basisAmount", "Basis amount",
                "export.column.percent", "Percent",
                "export.column.commission", "Commission")));

        byte[] bytes = service.exportStatement("emp-1", "2026-08", "en-US");

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        assertEquals(0x50, bytes[0]);
        assertEquals(0x4B, bytes[1]);
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
