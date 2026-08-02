package com.bemo.hr.trade.procurement;

import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.application.ProcurementService;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.domain.SupplierPayment;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SupplierPaymentConcurrencyTests {

    private final ProcurementService procurementService;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TenantApplicationRepository tenantApplicationRepository;
    private final BusinessPartyRepository businessPartyRepository;

    private final List<String> createdAppIds = new ArrayList<>();
    private final List<String> createdPartyIds = new ArrayList<>();
    private final List<String> createdInvoiceIds = new ArrayList<>();
    private final List<String> createdOperationIds = new ArrayList<>();

    @Autowired
    SupplierPaymentConcurrencyTests(ProcurementService procurementService,
                                    SupplierInvoiceRepository supplierInvoiceRepository,
                                    SupplierPaymentRepository supplierPaymentRepository,
                                    PartnerLedgerEntryRepository partnerLedgerEntryRepository,
                                    IdempotencyKeyRepository idempotencyKeyRepository,
                                    TenantApplicationRepository tenantApplicationRepository,
                                    BusinessPartyRepository businessPartyRepository) {
        this.procurementService = procurementService;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.partnerLedgerEntryRepository = partnerLedgerEntryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.businessPartyRepository = businessPartyRepository;
    }

    @AfterEach
    void cleanup() {
        try {
            if (!createdAppIds.isEmpty()) {
                TenantContext.set(createdAppIds.getLast());
                createdOperationIds.forEach(operationId ->
                        idempotencyKeyRepository.findByOperationTypeAndOperationId("SUPPLIER_PAYMENT", operationId)
                                .ifPresent(key -> idempotencyKeyRepository.deleteById(key.getId())));
                createdInvoiceIds.forEach(invoiceId -> {
                    createdPartyIds.forEach(partyId ->
                            partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(partyId)
                                    .forEach(entry -> partnerLedgerEntryRepository.deleteById(entry.getId())));
                    supplierPaymentRepository.findBySupplierInvoiceId(invoiceId)
                            .forEach(payment -> supplierPaymentRepository.deleteById(payment.getId()));
                    supplierInvoiceRepository.deleteById(invoiceId);
                });
            }
            businessPartyRepository.deleteAllById(createdPartyIds);
            tenantApplicationRepository.deleteAllById(createdAppIds);
        } finally {
            createdAppIds.clear();
            createdPartyIds.clear();
            createdInvoiceIds.clear();
            createdOperationIds.clear();
            TenantContext.clear();
        }
    }

    @Test
    void concurrentPaymentsWithDifferentOperationIdsCannotOverpayTheInvoice() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantApplication app = tenantApplicationRepository.save(
                new TenantApplication("CPAY-" + suffix, "Supplier payment concurrency test"));
        createdAppIds.add(app.getId());
        TenantContext.set(app.getId());

        BusinessParty supplier = businessPartyRepository.save(new BusinessParty(
                "SUPP-CONC-" + suffix, "Concurrency Supplier", null, "SUPPLIER",
                null, null, null, null, null, true,
                "DIRECT", null, null, null, "EGP", "E_INVOICE", "CASH", null, null));
        createdPartyIds.add(supplier.getId());

        SupplierInvoice invoice = supplierInvoiceRepository.save(new SupplierInvoice(
                "INV-CONC", "INV-CONC", null, "EGP", supplier.getId(), null,
                null, null, LocalDate.of(2026, 8, 1), new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null));
        createdInvoiceIds.add(invoice.getId());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Thread> workers = List.of(
                worker(app.getId(), invoice.getId(), supplier.getId(), "OP-CONC-1", "60.00", ready, start, succeeded, rejected),
                worker(app.getId(), invoice.getId(), supplier.getId(), "OP-CONC-2", "60.00", ready, start, succeeded, rejected));
        workers.forEach(Thread::start);

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Thread worker : workers) {
            worker.join(TimeUnit.SECONDS.toMillis(30));
            assertThat(worker.isAlive()).as("payment worker must finish").isFalse();
        }

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);

        BigDecimal totalPaid = supplierPaymentRepository.findBySupplierInvoiceId(invoice.getId()).stream()
                .filter(payment -> "POSTED".equals(payment.getStatus()))
                .map(SupplierPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPaid).isEqualByComparingTo("60.00");
        assertThat(supplierInvoiceRepository.findById(invoice.getId()).orElseThrow().getStatus())
                .isEqualTo("PARTIALLY_PAID");
    }

    private Thread worker(String appId, String invoiceId, String supplierId, String operationId, String amount,
                          CountDownLatch ready, CountDownLatch start,
                          AtomicInteger succeeded, AtomicInteger rejected) {
        return new Thread(() -> {
            TenantContext.set(appId);
            try {
                ready.countDown();
                start.await();
                long paymentDate = LocalDate.of(2026, 8, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                var payload = new ProcurementApi.SupplierPaymentPayload("PMT-" + operationId, paymentDate,
                        supplierId, invoiceId, new BigDecimal(amount), "BANK_TRANSFER", null, operationId);
                try {
                    procurementService.createSupplierPayment(payload);
                    succeeded.incrementAndGet();
                } catch (BusinessRuleException exception) {
                    rejected.incrementAndGet();
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } finally {
                createdOperationIds.add(operationId);
                TenantContext.clear();
            }
        });
    }
}
