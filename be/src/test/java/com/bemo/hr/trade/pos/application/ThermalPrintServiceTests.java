package com.bemo.hr.trade.pos.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.pos.api.ThermalPrinterApi;
import com.bemo.hr.trade.pos.domain.*;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionRepository;
import com.bemo.hr.trade.pos.infrastructure.ThermalPrinterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThermalPrintServiceTests {

    @Mock
    private ThermalPrinterRepository printerRepository;

    @Mock
    private PosTransactionRepository transactionRepository;

    @Mock
    private AuditService auditService;

    private ThermalPrintService service;

    @BeforeEach
    void setUp() {
        service = new ThermalPrintService(printerRepository, transactionRepository, auditService);
    }

    @Test
    void savesAndListsThermalPrinters() {
        when(printerRepository.save(any(ThermalPrinter.class))).thenAnswer(i -> i.getArgument(0));

        ThermalPrinterApi.SavePrinterRequest request = new ThermalPrinterApi.SavePrinterRequest(
                null,
                "Cashier 1 Printer",
                "branch-cairo",
                "terminal-01",
                ThermalPrinterConnectionType.NETWORK,
                "192.168.1.150",
                9100,
                null,
                ThermalPaperWidth.MM_80,
                "CP864",
                "BEMO CAIRO BRANCH",
                "Thank you!",
                true,
                true,
                true,
                true,
                true
        );

        ThermalPrinterApi.PrinterResponse response = service.savePrinter(request);
        assertThat(response.name()).isEqualTo("Cashier 1 Printer");
        assertThat(response.paperWidth()).isEqualTo(ThermalPaperWidth.MM_80);
        assertThat(response.connectionType()).isEqualTo(ThermalPrinterConnectionType.NETWORK);
        assertThat(response.ipAddress()).isEqualTo("192.168.1.150");
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void rejectsPrinterWithBlankName() {
        ThermalPrinterApi.SavePrinterRequest request = new ThermalPrinterApi.SavePrinterRequest(
                null, "   ", null, null, null, null, null, null, null, null, null, null, false, false, false, false, true
        );

        assertThatThrownBy(() -> service.savePrinter(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("name is required")
                .matches(e -> "THERMAL_PRINTER_NAME_REQUIRED".equals(((BusinessRuleException) e).getCode()));
    }

    @Test
    void generatesTestPrintPayload() {
        ThermalPrinter printer = new ThermalPrinter(
                "Counter Printer", "br-1", "term-1", ThermalPrinterConnectionType.USB,
                null, 9100, null, ThermalPaperWidth.MM_80, "CP864",
                "TEST STORE", "BYE", true, true, true, true
        );
        when(printerRepository.findById("printer-1")).thenReturn(Optional.of(printer));

        ThermalPrinterApi.TestPrintResponse response = service.testPrint("printer-1");
        assertThat(response.printerName()).isEqualTo("Counter Printer");
        assertThat(response.base64Bytes()).isNotEmpty();
    }

    @Test
    void generatesReceiptBytesFromPosTransaction() {
        PosTransaction txn = new PosTransaction(
                "REC-2026-00001", "sess-1", "term-1", "user-cashier",
                "cust-1", PosTransactionType.SALE, PosPaymentMethod.CASH,
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("70.00"), new BigDecimal("570.00"),
                new BigDecimal("600.00"), new BigDecimal("30.00"), null, null
        );

        PosTransactionLine line1 = new PosTransactionLine(
                txn.getId(), "item-1", "ITM-01", "Espresso Beans 1KG",
                new BigDecimal("1"), new BigDecimal("300.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("42.00"), new BigDecimal("300.00"), null
        );
        PosTransactionLine line2 = new PosTransactionLine(
                txn.getId(), "item-2", "ITM-02", "Paper Cups Box",
                new BigDecimal("2"), new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("28.00"), new BigDecimal("200.00"), null
        );
        txn.getLines().add(line1);
        txn.getLines().add(line2);

        when(transactionRepository.findById("txn-100")).thenReturn(Optional.of(txn));

        ThermalPrinterApi.ReceiptPrintDataResponse response = service.generateReceiptBytes("txn-100", null, false);
        assertThat(response.transactionNumber()).isEqualTo("REC-2026-00001");
        assertThat(response.base64Bytes()).isNotEmpty();
        assertThat(response.reprintCount()).isEqualTo(0);
    }

    @Test
    void reprintsReceiptAndLogsAuditTrail() {
        PosTransaction txn = new PosTransaction(
                "REC-2026-00002", "sess-1", "term-1", "user-cashier",
                "cust-1", PosTransactionType.SALE, PosPaymentMethod.CARD,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("14.00"), new BigDecimal("114.00"),
                null, BigDecimal.ZERO, null, null
        );
        when(transactionRepository.findById("txn-200")).thenReturn(Optional.of(txn));
        when(transactionRepository.save(any(PosTransaction.class))).thenAnswer(i -> i.getArgument(0));

        ThermalPrinterApi.ReprintReceiptRequest request = new ThermalPrinterApi.ReprintReceiptRequest(
                "Customer requested tax duplicate", null
        );

        ThermalPrinterApi.ReceiptPrintDataResponse response = service.reprintReceipt(
                "txn-200", request, "manager-user", "10.0.0.5"
        );

        assertThat(response.reprintCount()).isEqualTo(1);
        assertThat(txn.getReprintCount()).isEqualTo(1);
        assertThat(txn.getLastReprintedAt()).isNotNull();

        // Verify audit log
        verify(auditService).record(
                eq("POS_RECEIPT_REPRINT"),
                eq("PosTransaction"),
                eq(txn.getId()),
                eq("manager-user"),
                contains("Customer requested tax duplicate"),
                eq("10.0.0.5")
        );
    }

    @Test
    void rejectsReprintWithoutReason() {
        ThermalPrinterApi.ReprintReceiptRequest emptyReasonRequest = new ThermalPrinterApi.ReprintReceiptRequest(
                "   ", null
        );

        assertThatThrownBy(() -> service.reprintReceipt("txn-1", emptyReasonRequest, "user", "ip"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reason is required")
                .matches(e -> "POS_REPRINT_REASON_REQUIRED".equals(((BusinessRuleException) e).getCode()));
    }
}
