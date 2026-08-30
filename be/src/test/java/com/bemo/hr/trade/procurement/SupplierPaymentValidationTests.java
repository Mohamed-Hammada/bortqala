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
import com.bemo.hr.trade.procurement.infrastructure.*;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.verify;
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

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        supplierInvoiceRepository = mock(SupplierInvoiceRepository.class);
        supplierPaymentRepository = mock(SupplierPaymentRepository.class);
        tenantApplicationRepository = mock(TenantApplicationRepository.class);
        businessPartyRepository = mock(BusinessPartyRepository.class);
        documentSequenceRepository = mock(com.bemo.hr.shared.numbering.DocumentNumberSequenceRepository.class);
        documentNumberService = new com.bemo.hr.shared.numbering.DocumentNumberService(documentSequenceRepository);
        TenantContext.set("app-1");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("finance-officer",
                        "n/a", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_FINANCE_MANAGER"))));
        IdempotencyKeyRepository idempotencyKeyRepository = mock(IdempotencyKeyRepository.class);
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(1);
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        procurementService = new ProcurementService(mock(PurchaseOrderRepository.class),
                mock(PurchaseOrderLineRepository.class), mock(ProcurementDocumentSequenceRepository.class),
                mock(GoodsReceiptRepository.class), supplierInvoiceRepository, supplierPaymentRepository,
                mock(com.bemo.hr.trade.procurement.application.SupplierPaymentPlanService.class),
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
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("PROC_INVOICE_SUPPLIER_MISMATCH"));
    }

    @Test
    void rejectsPaymentAboveOutstandingBalance() {
        ProcurementApi.SupplierPaymentPayload payload = payload("supplier-a", new BigDecimal("100.01"));
        assertThatThrownBy(() -> procurementService.createSupplierPayment(payload))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("PROC_PAYMENT_EXCEEDS_BALANCE"));
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
    void discountedSettlementClosesInvoiceAndBooksDiscountEntry() {
        org.mockito.ArgumentCaptor<com.bemo.hr.operations.PartnerLedgerEntry> ledger =
                org.mockito.ArgumentCaptor.forClass(com.bemo.hr.operations.PartnerLedgerEntry.class);
        var ledgerRepo = mock(com.bemo.hr.operations.PartnerLedgerEntryRepository.class);
        var accountingService = mock(ProcurementAccountingService.class);
        // rebuild service with a capturable ledger repo (all other mocks reused)
        procurementService = new ProcurementService(mock(PurchaseOrderRepository.class),
                mock(PurchaseOrderLineRepository.class), mock(ProcurementDocumentSequenceRepository.class),
                mock(GoodsReceiptRepository.class), supplierInvoiceRepository, supplierPaymentRepository,
                mock(com.bemo.hr.trade.procurement.application.SupplierPaymentPlanService.class),
                mock(SupplierReturnRepository.class), businessPartyRepository, ledgerRepo,
                mock(AuditService.class), mock(ProcurementExcelExporter.class), mock(OperationsService.class),
                tenantApplicationRepository, mock(CurrencyRepository.class),
                new IdempotencyService(idempotencyKeyRepository()), mock(FiscalPeriodGuard.class),
                documentNumberService,
                mock(com.bemo.hr.trade.procurement.domain.ProcurementThreeWayMatchRepository.class),
                mock(com.bemo.hr.budget.application.BudgetService.class),
                accountingService);
        com.bemo.hr.shared.security.TenantApplication app = new com.bemo.hr.shared.security.TenantApplication("TEST", "Test App");
        when(tenantApplicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        when(supplierPaymentRepository.save(any(com.bemo.hr.trade.procurement.domain.SupplierPayment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentSequenceRepository.findByDocumentTypeAndYear("SUPPLIER_PAYMENT", 2026))
                .thenReturn(Optional.of(new com.bemo.hr.shared.numbering.DocumentNumberSequence("SUPPLIER_PAYMENT", 2026, 1)));

        ProcurementApi.SupplierPaymentPayload payload = new ProcurementApi.SupplierPaymentPayload(
                null, LocalDate.of(2026, 8, 6).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                "supplier-a", invoice.getId(), new BigDecimal("90.00"), new BigDecimal("10.00"),
                "BANK_TRANSFER", "settled for less", "op-disc");

        ProcurementApi.SupplierPaymentResponse response = procurementService.createSupplierPayment(payload);

        assertThat(response.settlementDiscount()).isEqualByComparingTo("10.00");
        assertThat(response.originalDue()).isEqualByComparingTo("100.00");
        assertThat(invoice.getStatus()).isEqualTo("PAID");
        // WP-02: the discount also reaches the GL subledger as its own balanced event
        verify(accountingService).postSupplierSettlementDiscount(any(), any(),
                org.mockito.ArgumentMatchers.refEq(new BigDecimal("10.00")), anyString());
        org.mockito.Mockito.verify(ledgerRepo, org.mockito.Mockito.times(2)).save(ledger.capture());
        assertThat(ledger.getAllValues().get(1).getEntryType()).isEqualTo("SUPPLIER_SETTLEMENT_DISCOUNT");
        assertThat(ledger.getAllValues().get(1).getAmountDelta()).isEqualByComparingTo("10.00");
        // paidAmount counts cash + discount, so the invoice reports fully paid on any further attempt
        assertThatThrownBy(() -> procurementService.createSupplierPayment(payload("supplier-a", new BigDecimal("0.01"))))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("PROC_INVOICE_ALREADY_PAID"));
    }

    @Test
    void rejectsNegativeSettlementDiscount() {
        ProcurementApi.SupplierPaymentPayload payload = new ProcurementApi.SupplierPaymentPayload(
                null, LocalDate.of(2026, 8, 6).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                "supplier-a", invoice.getId(), new BigDecimal("20.00"), new BigDecimal("-1.00"),
                "BANK_TRANSFER", null, "op-neg");
        assertThatThrownBy(() -> procurementService.createSupplierPayment(payload))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("PROC_SETTLEMENT_DISCOUNT_INVALID"));
    }

    @Test
    void rejectsSettlementDiscountWithoutFinanceRole() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("clerk",
                        "n/a", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PROCUREMENT_USER"))));
        ProcurementApi.SupplierPaymentPayload payload = new ProcurementApi.SupplierPaymentPayload(
                null, LocalDate.of(2026, 8, 6).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                "supplier-a", invoice.getId(), new BigDecimal("90.00"), new BigDecimal("10.00"),
                "BANK_TRANSFER", null, "op-forbidden");
        assertThatThrownBy(() -> procurementService.createSupplierPayment(payload))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException exception = (BusinessRuleException) ex;
                    assertThat(exception.getCode()).isEqualTo("PROC_SETTLEMENT_DISCOUNT_FORBIDDEN");
                    assertThat(exception.getStatus()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void rejectsDiscountedPaymentAboveOutstanding() {
        ProcurementApi.SupplierPaymentPayload payload = new ProcurementApi.SupplierPaymentPayload(
                null, LocalDate.of(2026, 8, 6).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                "supplier-a", invoice.getId(), new BigDecimal("95.00"), new BigDecimal("6.00"),
                "BANK_TRANSFER", null, "op-over");
        assertThatThrownBy(() -> procurementService.createSupplierPayment(payload))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("PROC_SETTLEMENT_DISCOUNT_EXCEEDS"));
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
                "supplier-a", invoice.getId(), new BigDecimal("50.00"), null, "BANK_TRANSFER", null, "op-auto");

        ProcurementApi.SupplierPaymentResponse response = procurementService.createSupplierPayment(payload);

        assertThat(response.paymentNumber()).isEqualTo("PMT-2026-00001");
    }

    private IdempotencyKeyRepository idempotencyKeyRepository() {
        IdempotencyKeyRepository repo = mock(IdempotencyKeyRepository.class);
        when(repo.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString())).thenReturn(1);
        when(repo.findByOperationTypeAndOperationId(anyString(), anyString())).thenReturn(Optional.empty());
        return repo;
    }

    private ProcurementApi.SupplierPaymentPayload payload(String supplierId, BigDecimal amount) {
        long paymentDate = LocalDate.of(2026, 7, 29).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new ProcurementApi.SupplierPaymentPayload("PMT-100", paymentDate, supplierId,
                invoice.getId(), amount, null, "BANK_TRANSFER", null, "op-1");
    }
}
