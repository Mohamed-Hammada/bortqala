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
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.application.ProcurementExcelExporter;
import com.bemo.hr.trade.procurement.application.ProcurementService;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import com.bemo.hr.trade.procurement.infrastructure.ProcurementDocumentSequenceRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderLineRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupplierPaymentValidationTests {
    private SupplierInvoiceRepository supplierInvoiceRepository;
    private SupplierPaymentRepository supplierPaymentRepository;
    private ProcurementService procurementService;
    private SupplierInvoice invoice;

    @BeforeEach
    void setUp() {
        supplierInvoiceRepository = mock(SupplierInvoiceRepository.class);
        supplierPaymentRepository = mock(SupplierPaymentRepository.class);
        IdempotencyKeyRepository idempotencyKeyRepository = mock(IdempotencyKeyRepository.class);
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(1);
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        procurementService = new ProcurementService(mock(PurchaseOrderRepository.class),
                mock(PurchaseOrderLineRepository.class), mock(ProcurementDocumentSequenceRepository.class),
                mock(GoodsReceiptRepository.class), supplierInvoiceRepository, supplierPaymentRepository,
                mock(BusinessPartyRepository.class), mock(PartnerLedgerEntryRepository.class),
                mock(AuditService.class), mock(ProcurementExcelExporter.class), mock(OperationsService.class),
                mock(TenantApplicationRepository.class), mock(CurrencyRepository.class),
                new IdempotencyService(idempotencyKeyRepository), mock(FiscalPeriodGuard.class));
        invoice = new SupplierInvoice("INV-100", "INV-100", null, "EGP", "supplier-a", null,
                null, null, LocalDate.of(2026, 7, 29), new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        when(supplierInvoiceRepository.findByIdForPayment(invoice.getId())).thenReturn(Optional.of(invoice));
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

    private ProcurementApi.SupplierPaymentPayload payload(String supplierId, BigDecimal amount) {
        long paymentDate = LocalDate.of(2026, 7, 29).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new ProcurementApi.SupplierPaymentPayload("PMT-100", paymentDate, supplierId,
                invoice.getId(), amount, "BANK_TRANSFER", null, "op-1");
    }
}
