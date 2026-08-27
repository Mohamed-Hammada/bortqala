package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.treasury.ChequeLayout;
import com.bemo.hr.finance.domain.treasury.CommercialCheque;
import com.bemo.hr.finance.infrastructure.ChequeLayoutRepository;
import com.bemo.hr.finance.infrastructure.CommercialChequeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChequePrintServiceTests {

    @Mock
    private CommercialChequeRepository chequeRepository;
    @Mock
    private ChequeLayoutRepository layoutRepository;

    @InjectMocks
    private ChequePrintService service;

    private CommercialCheque cheque;
    private ChequeLayout layout;

    @BeforeEach
    void setUp() {
        cheque = new CommercialCheque("CHQ-001", CommercialCheque.ChequeType.ISSUED, "CIB", null,
                "Ahmed Mohamed", null, new BigDecimal("15000.50"), "EGP",
                System.currentTimeMillis(), System.currentTimeMillis(), null);

        layout = new ChequeLayout("CIB", "Commercial International Bank",
                50, 80, 400, 550, 30, 50, 130, 450, 520, 130, 120, 550, 80, true);
    }

    @Test
    void getPrintDataReturnsChequeFieldsWithAmountInWords() {
        when(chequeRepository.findById("chq-1")).thenReturn(Optional.of(cheque));
        when(layoutRepository.findByBankCode("CIB")).thenReturn(Optional.of(layout));

        ChequePrintService.ChequePrintData data = service.getPrintData("chq-1");

        assertEquals("Ahmed Mohamed", data.drawerPayeeName());
        assertTrue(data.amountInWords().contains("ج.م"));
        assertTrue(data.amountInWords().length() > 5);
        assertEquals("15,000.50", data.amountInDigits());
        assertEquals("EGP", data.currency());
        assertNotNull(data.layout());
    }

    @Test
    void getPrintDataThrowsWhenChequeNotFound() {
        when(chequeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> service.getPrintData("missing"));
    }

    @Test
    void getLayoutThrowsWhenLayoutNotFound() {
        when(layoutRepository.findByBankCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> service.getLayout("UNKNOWN"));
    }

    @Test
    void renderPrintViewProducesHtmlWithAmountInWords() {
        when(chequeRepository.findById("chq-1")).thenReturn(Optional.of(cheque));
        when(layoutRepository.findByBankCode("CIB")).thenReturn(Optional.of(layout));

        String html = service.renderPrintView("chq-1");

        assertTrue(html.contains("Ahmed Mohamed"));
        assertTrue(html.contains("ج.م"));
        assertTrue(html.contains("15,000.50"));
        assertTrue(html.contains("CIB"));
        assertTrue(html.contains("CHQ-001"));
        assertTrue(html.contains("<!DOCTYPE html>"));
    }

    @Test
    void renderPrintViewUsesDefaultCoordinatesWhenNoLayout() {
        when(chequeRepository.findById("chq-1")).thenReturn(Optional.of(cheque));
        when(layoutRepository.findByBankCode("CIB")).thenReturn(Optional.empty());

        String html = service.renderPrintView("chq-1");

        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Ahmed Mohamed"));
    }
}
