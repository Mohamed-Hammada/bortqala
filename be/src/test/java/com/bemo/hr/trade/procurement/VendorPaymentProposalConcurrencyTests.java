package com.bemo.hr.trade.procurement;

import com.bemo.hr.PostgresIntegrationTest;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.domain.posting.PostingProfile;
import com.bemo.hr.finance.domain.posting.PostingProfileLine;
import com.bemo.hr.finance.domain.posting.PostingProfileLineRepository;
import com.bemo.hr.finance.domain.posting.PostingProfileRepository;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.numbering.DocumentNumberSequenceRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.application.VendorPaymentProposalService;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalAllocationRepository;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class VendorPaymentProposalConcurrencyTests extends PostgresIntegrationTest {

    @Autowired
    private VendorPaymentProposalService proposalService;
    @Autowired
    private VendorPaymentProposalRepository proposalRepository;
    @Autowired
    private VendorPaymentProposalAllocationRepository allocationRepository;
    @Autowired
    private SupplierInvoiceRepository invoiceRepository;
    @Autowired
    private SupplierPaymentRepository paymentRepository;
    @Autowired
    private PartnerLedgerEntryRepository ledgerRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private BusinessPartyRepository partyRepository;
    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired
    private TenantApplicationRepository appRepository;
    @Autowired
    private DocumentNumberSequenceRepository documentNumberSequenceRepository;
    @Autowired
    private PostingProfileRepository postingProfileRepository;
    @Autowired
    private PostingProfileLineRepository postingProfileLineRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private String appId;
    private String supplierId;
    private String invoiceId;
    private String proposalId;
    private String fiscalPeriodId;
    private String postingProfileId;

    @AfterEach
    void cleanup() {
        try {
            if (appId == null) return;
            TenantContext.set(appId);
            if (proposalId != null) {
                auditLogRepository.search("VENDOR_PAYMENT_PROPOSAL", null, null, proposalId,
                        null, null, PageRequest.of(0, 20)).forEach(auditLogRepository::delete);
                allocationRepository.findByProposalIdOrderByLineNoAsc(proposalId).forEach(allocationRepository::delete);
                proposalRepository.deleteById(proposalId);
            }
            if (supplierId != null)
                ledgerRepository.findByPartyIdOrderByOccurredAtDesc(supplierId).forEach(ledgerRepository::delete);
            if (invoiceId != null) {
                paymentRepository.findBySupplierInvoiceId(invoiceId).forEach(paymentRepository::delete);
                invoiceRepository.deleteById(invoiceId);
            }
            if (supplierId != null) partyRepository.deleteById(supplierId);
            if (fiscalPeriodId != null) fiscalPeriodRepository.deleteById(fiscalPeriodId);
            if (postingProfileId != null) {
                postingProfileLineRepository.findByProfileIdOrderByLineNoAsc(postingProfileId)
                        .forEach(line -> postingProfileLineRepository.deleteById(line.getId()));
                postingProfileRepository.deleteById(postingProfileId);
            }
            // findByDocumentTypeAndYear takes a pessimistic write lock and requires an active
            // transaction; this plain @AfterEach method has none, so wrap just this lookup+delete.
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    documentNumberSequenceRepository.findByDocumentTypeAndYear("SUPPLIER_PAYMENT", LocalDate.now().getYear())
                            .ifPresent(documentNumberSequenceRepository::delete));
            appRepository.deleteById(appId);
        } finally {
            TenantContext.clear();
        }
    }

    @RepeatedTest(5)
    void concurrentSameOperationExecutionCreatesOnePaymentAndOneLedgerEffect() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TenantApplication app = appRepository.save(new TenantApplication("PROP-" + suffix, "Proposal concurrency"));
        appId = app.getId();
        TenantContext.set(appId);
        LocalDate today = LocalDate.now();
        FiscalPeriod period = fiscalPeriodRepository.save(new FiscalPeriod(today.getYear(), today.getMonthValue(),
                "Proposal concurrency", today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()),
                FiscalPeriod.Status.OPEN));
        fiscalPeriodId = period.getId();
        BusinessParty supplier = partyRepository.save(new BusinessParty("SUP-" + suffix, "Concurrent Supplier", null,
                "SUPPLIER", null, null, null, null, null, true, "DIRECT", null, null, null,
                "EGP", "E_INVOICE", "CASH", null, "EG123456789012345678901234"));
        supplierId = supplier.getId();
        SupplierInvoice invoice = invoiceRepository.save(new SupplierInvoice("INV-" + suffix, "INV-" + suffix,
                null, "EGP", supplierId, null, null, null, today, new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, today, null));
        invoiceId = invoice.getId();

        // A brand-new tenant has no posting profile configured; executeProposal ultimately posts a
        // supplier payment to the subledger and requires a profile for "SUPPLIER_PAYMENT_BANK_TRANSFER"
        // (the businessEvent built from the BANK_TRANSFER method used below). Without this, every
        // concurrent execution attempt fails with SUBLEDGER_POSTING_PROFILE_REQUIRED before the
        // concurrency-conflict logic under test is ever exercised.
        PostingProfile profile = postingProfileRepository.save(
                new PostingProfile("PROP-PROFILE", "SUPPLIER_PAYMENT_BANK_TRANSFER", today.minusYears(1), null));
        postingProfileId = profile.getId();
        postingProfileLineRepository.save(new PostingProfileLine(profile.getId(), 1, "DEBIT", "FIXED", UUID.randomUUID().toString(), "AMOUNT"));
        postingProfileLineRepository.save(new PostingProfileLine(profile.getId(), 2, "CREDIT", "FIXED", UUID.randomUUID().toString(), "AMOUNT"));

        var created = proposalService.createProposal(supplierId,
                List.of(new VendorPaymentProposalService.AllocationInput(invoiceId, new BigDecimal("60.00"))),
                today, "maker");
        proposalId = created.id();
        proposalService.approveProposal(proposalId, "checker");
        TenantContext.clear();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<String> results = new CopyOnWriteArrayList<>();
        List<Thread> workers = List.of(
                worker(ready, start, failures, results), worker(ready, start, failures, results));
        workers.forEach(Thread::start);
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Thread worker : workers) {
            worker.join(TimeUnit.SECONDS.toMillis(30));
            assertThat(worker.isAlive()).isFalse();
        }

        TenantContext.set(appId);
        assertThat(failures).isEmpty();
        assertThat(results).containsExactlyInAnyOrder(proposalId, proposalId);
        assertThat(paymentRepository.findBySupplierInvoiceId(invoiceId)).hasSize(1)
                .singleElement().satisfies(payment -> assertThat(payment.getAmount()).isEqualByComparingTo("60.00"));
        assertThat(ledgerRepository.findByPartyIdOrderByOccurredAtDesc(supplierId)).hasSize(1);
        assertThat(allocationRepository.findByProposalIdOrderByLineNoAsc(proposalId)).singleElement()
                .satisfies(allocation -> assertThat(allocation.getSupplierPaymentId()).isNotBlank());
    }

    private Thread worker(CountDownLatch ready, CountDownLatch start, List<Throwable> failures, List<String> results) {
        return new Thread(() -> {
            TenantContext.set(appId);
            try {
                ready.countDown();
                start.await();
                results.add(proposalService.executeProposal(proposalId, "same-operation", "BANK_TRANSFER", "disburser").id());
            } catch (BusinessRuleException exception) {
                failures.add(exception);
            } catch (Throwable throwable) {
                failures.add(throwable);
            } finally {
                TenantContext.clear();
            }
        });
    }
}
