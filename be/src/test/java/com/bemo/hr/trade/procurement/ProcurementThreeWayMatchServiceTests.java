package com.bemo.hr.trade.procurement;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.infrastructure.CurrencyRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.application.ProcurementAccountingService;
import com.bemo.hr.trade.procurement.application.ProcurementExcelExporter;
import com.bemo.hr.trade.procurement.application.ProcurementService;
import com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatch;
import com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatchRepository;
import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcurementThreeWayMatchServiceTests {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderLineRepository purchaseOrderLineRepository;
    @Mock
    private ProcurementDocumentSequenceRepository procurementDocumentSequenceRepository;
    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;
    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock
    private SupplierPaymentRepository supplierPaymentRepository;
    @Mock
    private BusinessPartyRepository businessPartyRepository;
    @Mock
    private PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private ProcurementExcelExporter procurementExcelExporter;
    @Mock
    private OperationsService operationsService;
    @Mock
    private TenantApplicationRepository tenantApplicationRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private SupplierReturnRepository supplierReturnRepository;
    @Mock
    private FiscalPeriodGuard fiscalPeriodGuard;
    @Mock
    private ProcurementThreeWayMatchRepository threeWayMatchRepository;
    @Mock
    private com.bemo.hr.budget.application.BudgetService budgetService;

    private ProcurementService procurementService;

    @BeforeEach
    void setUp() {
        procurementService = new ProcurementService(
                purchaseOrderRepository, purchaseOrderLineRepository, procurementDocumentSequenceRepository,
                goodsReceiptRepository, supplierInvoiceRepository, supplierPaymentRepository,
                mock(com.bemo.hr.trade.procurement.application.SupplierPaymentPlanService.class),
                supplierReturnRepository, businessPartyRepository, partnerLedgerEntryRepository, auditService,
                procurementExcelExporter, operationsService, tenantApplicationRepository,
                currencyRepository, idempotencyService, fiscalPeriodGuard,
                new com.bemo.hr.shared.numbering.DocumentNumberService(
                        mock(com.bemo.hr.shared.numbering.DocumentNumberSequenceRepository.class)),
                threeWayMatchRepository, budgetService,
                mock(ProcurementAccountingService.class)
        );
    }

    @Test
    void performThreeWayMatch_whenWithinTolerance_setsStatusMatched() {
        SupplierInvoice invoice = mock(SupplierInvoice.class);
        when(invoice.getId()).thenReturn("INV-001");
        when(invoice.getPurchaseOrderId()).thenReturn("PO-100");
        when(invoice.getNetAmount()).thenReturn(new BigDecimal("1000.00"));

        PurchaseOrder po = mock(PurchaseOrder.class);
        when(po.getId()).thenReturn("PO-100");
        when(po.getTotalAmount()).thenReturn(new BigDecimal("1000.00"));

        when(supplierInvoiceRepository.findById("INV-001")).thenReturn(Optional.of(invoice));
        when(purchaseOrderRepository.findById("PO-100")).thenReturn(Optional.of(po));
        when(threeWayMatchRepository.findBySupplierInvoiceId("INV-001")).thenReturn(Optional.empty());
        when(threeWayMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcurementApi.ThreeWayMatchResponse result = procurementService.performThreeWayMatch("INV-001", BigDecimal.ZERO);

        assertThat(result.matchStatus()).isEqualTo("MATCHED");
        assertThat(result.priceVarianceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void performThreeWayMatch_whenPriceExceedsTolerance_setsStatusVarianceExceeded() {
        SupplierInvoice invoice = mock(SupplierInvoice.class);
        when(invoice.getId()).thenReturn("INV-002");
        when(invoice.getPurchaseOrderId()).thenReturn("PO-100");
        when(invoice.getNetAmount()).thenReturn(new BigDecimal("1200.00"));

        PurchaseOrder po = mock(PurchaseOrder.class);
        when(po.getId()).thenReturn("PO-100");
        when(po.getTotalAmount()).thenReturn(new BigDecimal("1000.00"));

        when(supplierInvoiceRepository.findById("INV-002")).thenReturn(Optional.of(invoice));
        when(purchaseOrderRepository.findById("PO-100")).thenReturn(Optional.of(po));
        when(threeWayMatchRepository.findBySupplierInvoiceId("INV-002")).thenReturn(Optional.empty());
        when(threeWayMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcurementApi.ThreeWayMatchResponse result = procurementService.performThreeWayMatch("INV-002", new BigDecimal("5.00"));

        assertThat(result.matchStatus()).isEqualTo("VARIANCE_EXCEEDED");
        assertThat(result.priceVarianceAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void resolveMatchVariance_updatesStatusToResolved() {
        ProcurementThreeWayMatch match = new ProcurementThreeWayMatch("PO-100", "GRN-100", "INV-001",
                "VARIANCE_EXCEEDED", new BigDecimal("200.00"), BigDecimal.ZERO, new BigDecimal("5.00"), "Price mismatch");

        when(threeWayMatchRepository.findById("MATCH-001")).thenReturn(Optional.of(match));
        when(threeWayMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcurementApi.ThreeWayMatchResponse result = procurementService.resolveMatchVariance("MATCH-001", "Approved exception by Manager");

        assertThat(result.matchStatus()).isEqualTo("RESOLVED");
        assertThat(result.varianceReason()).contains("Approved exception by Manager");
    }
}
