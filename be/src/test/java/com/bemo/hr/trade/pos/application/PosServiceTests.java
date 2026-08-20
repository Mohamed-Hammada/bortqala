package com.bemo.hr.trade.pos.application;

import com.bemo.hr.trade.pos.api.PosApi;
import com.bemo.hr.trade.pos.domain.*;
import com.bemo.hr.trade.pos.infrastructure.PosSessionRepository;
import com.bemo.hr.trade.pos.infrastructure.PosTerminalRepository;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionLineRepository;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PosServiceTests {

    @Mock
    private PosTerminalRepository terminalRepository;

    @Mock
    private PosSessionRepository sessionRepository;

    @Mock
    private PosTransactionRepository transactionRepository;

    @Mock
    private PosTransactionLineRepository transactionLineRepository;

    private PosService service;

    @BeforeEach
    void setUp() {
        service = new PosService(terminalRepository, sessionRepository, transactionRepository, transactionLineRepository);
    }

    @Test
    void savesAndListsTerminals() {
        when(terminalRepository.findByTerminalCode("POS-01")).thenReturn(Optional.empty());
        when(terminalRepository.save(any(PosTerminal.class))).thenAnswer(i -> i.getArgument(0));
        when(terminalRepository.findAllByOrderByTerminalCodeAsc()).thenReturn(List.of(
                new PosTerminal("POS-01", "Main Counter Register", "branch-1", "wh-1", "cashbox-1")
        ));

        PosApi.SaveTerminalRequest request = new PosApi.SaveTerminalRequest(
                "POS-01", "Main Counter Register", "branch-1", "wh-1", "cashbox-1", PosTerminalStatus.ACTIVE
        );

        PosApi.TerminalResponse response = service.saveTerminal(request);
        assertThat(response.terminalCode()).isEqualTo("POS-01");
        assertThat(response.terminalName()).isEqualTo("Main Counter Register");

        List<PosApi.TerminalResponse> list = service.listTerminals();
        assertThat(list).hasSize(1);
    }

    @Test
    void opensSessionWithOpeningFloat() {
        PosTerminal terminal = new PosTerminal("POS-01", "Main Counter", "b1", "w1", "c1");
        when(terminalRepository.findById("term-1")).thenReturn(Optional.of(terminal));
        when(sessionRepository.findFirstByTerminalIdAndStatus(terminal.getId(), PosSessionStatus.OPEN)).thenReturn(Optional.empty());
        when(sessionRepository.count()).thenReturn(0L);
        when(sessionRepository.save(any(PosSession.class))).thenAnswer(i -> i.getArgument(0));

        PosApi.OpenSessionRequest request = new PosApi.OpenSessionRequest("term-1", new BigDecimal("500.00"));
        PosApi.SessionResponse response = service.openSession("cashier-user-1", request);

        assertThat(response.sessionNumber()).contains("POS-SES-");
        assertThat(response.openingFloat()).isEqualByComparingTo("500.00");
        assertThat(response.status()).isEqualTo(PosSessionStatus.OPEN);
    }

    @Test
    void processesSaleWith14PercentVatAndCalculatesCashChange() {
        PosSession session = new PosSession("POS-SES-2026-001", "term-1", "cashier-1", new BigDecimal("500.00"));
        when(sessionRepository.findById("ses-1")).thenReturn(Optional.of(session));
        when(transactionRepository.count()).thenReturn(0L);
        when(transactionRepository.save(any(PosTransaction.class))).thenAnswer(i -> i.getArgument(0));

        List<PosApi.PosLineItem> lines = List.of(
                new PosApi.PosLineItem("item-1", "BAR-101", "Espresso Beans 1KG", new BigDecimal("2"), new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null)
        );

        PosApi.ProcessSaleRequest request = new PosApi.ProcessSaleRequest(
                "ses-1", "cust-1", PosPaymentMethod.CASH, new BigDecimal("250.00"), "off-123", lines
        );

        PosApi.TransactionResponse response = service.processSale("cashier-1", request);

        // Subtotal = 200.00, VAT 14% = 28.00, Total = 228.00
        assertThat(response.subtotal()).isEqualByComparingTo("200.00");
        assertThat(response.taxAmount()).isEqualByComparingTo("28.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("228.00");
        assertThat(response.cashTendered()).isEqualByComparingTo("250.00");
        assertThat(response.changeAmount()).isEqualByComparingTo("22.00");
        assertThat(response.status()).isEqualTo(PosTransactionStatus.COMPLETED);

        // Session calculated cash = 500 + 228 = 728.00
        assertThat(session.getClosingCalculatedCash()).isEqualByComparingTo("728.00");
    }

    @Test
    void reconcilesAndClosesSessionWithVarianceCalculation() {
        PosSession session = new PosSession("POS-SES-2026-001", "term-1", "cashier-1", new BigDecimal("500.00"));
        session.addSaleTotals(new BigDecimal("300.00"), new BigDecimal("150.00"));
        // calculated cash = 800.00, calculated card = 150.00

        when(sessionRepository.findById("ses-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(PosSession.class))).thenAnswer(i -> i.getArgument(0));

        PosApi.CloseSessionRequest request = new PosApi.CloseSessionRequest(
                new BigDecimal("810.00"), // +10 cash variance
                new BigDecimal("150.00"), // 0 card variance
                "Shift closed smoothly, 10 EGP surplus"
        );

        PosApi.SessionResponse response = service.closeSession("ses-1", request);

        assertThat(response.status()).isEqualTo(PosSessionStatus.CLOSED);
        assertThat(response.closingActualCash()).isEqualByComparingTo("810.00");
        assertThat(response.cashVariance()).isEqualByComparingTo("10.00");
        assertThat(response.cardVariance()).isEqualByComparingTo("0.00");
    }

    @Test
    void processesReturnAndRestoresSessionTotals() {
        PosSession session = new PosSession("POS-SES-2026-001", "term-1", "cashier-1", new BigDecimal("500.00"));
        session.addSaleTotals(new BigDecimal("228.00"), BigDecimal.ZERO);

        PosTransaction original = new PosTransaction(
                "POS-TXN-2026-0001", "ses-1", "term-1", "cashier-1", "cust-1",
                PosTransactionType.SALE, PosPaymentMethod.CASH, new BigDecimal("200.00"),
                BigDecimal.ZERO, new BigDecimal("28.00"), new BigDecimal("228.00"),
                new BigDecimal("250.00"), new BigDecimal("22.00"), null, null
        );

        when(transactionRepository.findById("txn-1")).thenReturn(Optional.of(original));
        when(sessionRepository.findById("ses-1")).thenReturn(Optional.of(session));
        when(transactionRepository.count()).thenReturn(1L);
        when(transactionRepository.save(any(PosTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PosApi.ProcessReturnRequest request = new PosApi.ProcessReturnRequest("txn-1", "ses-1", "Damaged goods", List.of());
        PosApi.TransactionResponse response = service.processReturn("cashier-1", request);

        assertThat(response.transactionType()).isEqualTo(PosTransactionType.RETURN);
        assertThat(response.totalAmount()).isEqualByComparingTo("-228.00");
        assertThat(session.getClosingCalculatedCash()).isEqualByComparingTo("500.00");
        assertThat(original.getStatus()).isEqualTo(PosTransactionStatus.REFUNDED);
    }
}
