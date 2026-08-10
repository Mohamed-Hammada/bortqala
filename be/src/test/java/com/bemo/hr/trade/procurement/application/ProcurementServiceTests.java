package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.budget.application.BudgetService;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.domain.GoodsReceipt;
import com.bemo.hr.trade.procurement.domain.GoodsReceiptLine;
import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import com.bemo.hr.trade.procurement.domain.PurchaseOrderLine;
import com.bemo.hr.trade.procurement.domain.SupplierReturn;
import com.bemo.hr.trade.procurement.domain.SupplierReturnLine;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderLineRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierReturnRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcurementServiceTests {

    @BeforeEach
    void setUp() {
        TenantContext.set("test-tenant");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderLineRepository purchaseOrderLineRepository;
    @Mock private GoodsReceiptRepository goodsReceiptRepository;
    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private SupplierPaymentRepository supplierPaymentRepository;
    @Mock private SupplierReturnRepository supplierReturnRepository;
    @Mock private BusinessPartyRepository businessPartyRepository;
    @Mock private PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    @Mock private AuditService auditService;
    @Mock private ProcurementExcelExporter procurementExcelExporter;
    @Mock private OperationsService operationsService;
    @Mock private TenantApplicationRepository tenantApplicationRepository;
    @Mock private com.bemo.hr.finance.infrastructure.CurrencyRepository currencyRepository;
    @Mock private com.bemo.hr.shared.idempotency.application.IdempotencyService idempotencyService;
    @Mock private FiscalPeriodGuard fiscalPeriodGuard;
    @Mock private DocumentNumberService documentNumberService;
    @Mock private com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatchRepository threeWayMatchRepository;
    @Mock private BudgetService budgetService;
    @Mock private com.bemo.hr.trade.procurement.infrastructure.ProcurementDocumentSequenceRepository procurementDocumentSequenceRepository;

    @InjectMocks
    private ProcurementService service;

    @Test
    void directReceive_throwsDeprecatedError() {
        assertThatThrownBy(() -> service.receive("po-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Goods Receipt")
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo("PROC_DIRECT_RECEIVE_DEPRECATED");
    }

    @Test
    void cancel_withExistingReceipts_throwsError() {
        PurchaseOrder po = new PurchaseOrder("PO-100", LocalDate.now(), "supp-1", null, "NET30", "EGP", BigDecimal.TEN);
        when(purchaseOrderRepository.findById(po.getId())).thenReturn(Optional.of(po));

        GoodsReceiptLine line = new GoodsReceiptLine("pol-1", "item-1", "Item 1", "CAT", BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10), "EA", BigDecimal.TEN, null, null, null);
        GoodsReceipt grn = new GoodsReceipt("GRN-100", LocalDate.now(), po.getId(), "supp-1", null, null, List.of(line));
        when(goodsReceiptRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of(grn));

        assertThatThrownBy(() -> service.cancel(po.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo("PO_HAS_RECEIPTS_CANNOT_CANCEL");
    }

    @Test
    void createSupplierReturn_validReturn_postsStockAndRecalculatesStatus() {
        PurchaseOrder po = new PurchaseOrder("PO-100", LocalDate.now(), "supp-1", null, "NET30", "EGP", BigDecimal.TEN);
        PurchaseOrderLine poLine = new PurchaseOrderLine(po.getId(), "item-1", "Item 1", "CAT", BigDecimal.valueOf(10), "EA", BigDecimal.TEN);
        when(purchaseOrderRepository.findById(po.getId())).thenReturn(Optional.of(po));
        when(purchaseOrderLineRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of(poLine));

        GoodsReceiptLine grnLine = new GoodsReceiptLine(poLine.getId(), "item-1", "Item 1", "CAT", BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10), "EA", BigDecimal.TEN, null, null, null);
        GoodsReceipt grn = new GoodsReceipt("GRN-100", LocalDate.now(), po.getId(), "supp-1", null, null, List.of(grnLine));
        when(goodsReceiptRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of(grn));
        TenantApplication tenantApp = new TenantApplication("test-tenant", "Test App");
        when(tenantApplicationRepository.findById("test-tenant")).thenReturn(Optional.of(tenantApp));
        when(documentNumberService.next(eq("SUPPLIER_RETURN"), eq("RET"), any())).thenReturn("RET-100");
        when(supplierReturnRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of());
        when(supplierReturnRepository.save(any(SupplierReturn.class))).thenAnswer(i -> i.getArgument(0));

        ProcurementApi.SupplierReturnLinePayload linePayload = new ProcurementApi.SupplierReturnLinePayload(
                poLine.getId(), "item-1", "Item 1", "CAT", BigDecimal.valueOf(4), "EA", BigDecimal.TEN, null, "Defective item");
        ProcurementApi.SupplierReturnPayload payload = new ProcurementApi.SupplierReturnPayload(
                "RET-100", System.currentTimeMillis(), po.getId(), "supp-1", null, "Return notes", List.of(linePayload));

        ProcurementApi.SupplierReturnResponse response = service.createSupplierReturn(payload);

        assertThat(response).isNotNull();
        assertThat(response.returnNumber()).isEqualTo("RET-100");
        assertThat(po.getStatus()).isEqualTo(PurchaseOrder.Status.PARTIALLY_RECEIVED);
        verify(operationsService).recordSupplierReturn(eq("item-1"), eq("supp-1"), eq(BigDecimal.valueOf(4)), eq(BigDecimal.TEN), eq("RET-100"), eq("Defective item"), any(), any());
    }
}
