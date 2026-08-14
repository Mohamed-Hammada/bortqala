package com.bemo.hr.trade.procurement;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.infrastructure.CurrencyRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.application.ProcurementAccountingService;
import com.bemo.hr.trade.procurement.application.ProcurementExcelExporter;
import com.bemo.hr.trade.procurement.application.ProcurementService;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementDocumentSequenceRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderLineRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupplierPaymentValidationTests {
    private SupplierInvoiceRepository supplierInvoiceRepository;
    private SupplierPaymentRepository supplierPaymentRepository;
    private TenantApplicationRepository tenantApplicationRepository;
    private BusinessPartyRepository businessPartyRepository;
    private com.bemo.hr.shared.numbering.DocumentNumberSequenceRepository documentSequenceRepository;
    private com.bemo.hr.shared.numbering.DocumentNumberService documentNumberService;
    private ProcurementService procurementService;
    private SupplierInvoice invoice;

    @BeforeEach
    void setUp() {
        supplierInvoiceRepository = mock(SupplierInvoiceRepository.class);
        supplierPaymentRepository = mock(SupplierPaymentRepository.class);
        tenantApplicationRepository = mock(TenantApplicationRepository.class);
        businessPartyRepository = mock(BusinessPartyRepository.class);
        documentSequenceRepository = mock(com.bemo.hr.shared.numbering.DocumentNumberSequenceRepository.class);
        documentNumberService = new com.bemo.hr.shared.numbering.DocumentNumberService(documentSequenceRepository);
        TenantContext.set("app-1");
        IdempotencyKeyRepository idempotencyKeyRepository = mock(IdempotencyKeyRepository.class);
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(1);
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        procurementService = new ProcurementService(mock(PurchaseOrderRepository.class),
                mock(PurchaseOrderLineRepository.class), mock(ProcurementDocumentSequenceRepository.class),
                mock(GoodsReceiptRepository.class), supplierInvoiceRepository, supplierPaymentRepository,
                mock(SupplierReturnRepository.class), businessPartyRepository, mock(PartnerLedgerEntryRepository.class),
                mock(AuditService.class), mock(ProcurementExcelExporter.class), mock(OperationsService.class),
                tenantApplicationRepository, mock(CurrencyRepository.class),
                new IdempotencyService(idempotencyKeyRepository), mock(FiscalPeriodGuard.class),
                documentNumberService,
                mock(com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatchRepository.class),
                mock(com.bemo.hr.budget.application.BudgetService.class),
                mock(ProcurementAccountingService.class));
        invoice = new SupplierInvoice("INV-100", "INV-100", null, "EGP", "supplier-a", null,
                null, null, LocalDate.of(2026, 7, 29), new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        when(supplierInvoiceRepository.findByIdForPayment(invoice.getId())).thenReturn(Optional.of(invoice));
        when(businessPartyRepository.findById("supplier-a"))
                .thenReturn(Optional.of(new com.bemo.hr.party.BusinessParty("supplier-a", "مورد أ", null,
                        "SUPPLIER", null, null, null, null, null, true,
                        null, null, null, null, null, null, null, null, "EG123456789012345678901234")));
        when(supplierPaymentRepository.findByOperationId("op-1")).thenReturn(Optional.empty());
        when(supplierPaymentRepository.findBySupplierInvoiceId(invoice.getId())).thenReturn(List.of());
    }

    @Test
    void rejectsInvoiceBelongingToAnotherSupplier() {
        ProcurementApi.SupplierPaymentPayload payload = payload("supplier-b", new BigDecimal("20.00"));
        assertThatThrownBy(() -> procurementService.createSupplierPayment(payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("لا تخص المورد");
    }

    @Test
    void rejectsPaymentAboveOutstandingBalance() {
        ProcurementApi.SupplierPaymentPayload payload = payload("supplier-a", new BigDecimal("100.01"));
        assertThatThrownBy(() -> procurementService.createSupplierPayment(payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("يتجاوز الرصيد المتبقي");
    }

    @Test
    void rejectsPaymentWhenSupplierBankIsNotVerified() {
        when(businessPartyRepository.findById("supplier-a"))
                .thenReturn(Optional.of(new com.bemo.hr.party.BusinessParty("supplier-a", "Supplier A", null,
                        "SUPPLIER", null, null, null, null, null, true,
                        null, null, null, null, "EGP", "E_INVOICE", "CASH", "TAX12345", null)));

        assertThatThrownBy(() -> procurementService.createSupplierPayment(payload("supplier-a", new BigDecimal("20.00"))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo("PROC_SUPPLIER_BANK_VERIFICATION_REQUIRED");
    }

    @Test
    void autoGeneratesPaymentNumberWhenDocumentNumberingEnabled() {
        com.bemo.hr.shared.security.TenantApplication app = new com.bemo.hr.shared.security.TenantApplication("TEST", "Test App");
        when(tenantApplicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        when(supplierPaymentRepository.findByOperationId("op-auto")).thenReturn(Optional.empty());
        when(supplierPaymentRepository.save(any(com.bemo.hr.trade.procurement.domain.SupplierPayment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentSequenceRepository.findByDocumentTypeAndYear("SUPPLIER_PAYMENT", 2026))
                .thenReturn(Optional.of(new com.bemo.hr.shared.numbering.DocumentNumberSequence("SUPPLIER_PAYMENT", 2026, 1)));

        ProcurementApi.SupplierPaymentPayload payload = new ProcurementApi.SupplierPaymentPayload(
                null, LocalDate.of(2026, 8, 6).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                "supplier-a", invoice.getId(), new BigDecimal("50.00"), "BANK_TRANSFER", null, "op-auto");

        ProcurementApi.SupplierPaymentResponse response = procurementService.createSupplierPayment(payload);

        assertThat(response.paymentNumber()).isEqualTo("PMT-2026-00001");
    }

    private ProcurementApi.SupplierPaymentPayload payload(String supplierId, BigDecimal amount) {
        long paymentDate = LocalDate.of(2026, 7, 29).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new ProcurementApi.SupplierPaymentPayload("PMT-100", paymentDate, supplierId,
                invoice.getId(), amount, "BANK_TRANSFER", null, "op-1");
    }
}
