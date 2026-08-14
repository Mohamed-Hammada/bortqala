package com.bemo.hr.trade.procurement;

import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.application.VendorPaymentProposalService;
import com.bemo.hr.trade.procurement.domain.SupplierInvoice;
import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import com.bemo.hr.trade.procurement.infrastructure.SupplierInvoiceRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierPaymentRepository;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalAllocationRepository;
import com.bemo.hr.trade.procurement.infrastructure.VendorPaymentProposalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VendorPaymentProposalPersistenceTests {

    @Autowired private VendorPaymentProposalService vendorPaymentProposalService;
    @Autowired private VendorPaymentProposalAllocationRepository allocationRepository;
    @Autowired private VendorPaymentProposalRepository proposalRepository;
    @Autowired private SupplierInvoiceRepository supplierInvoiceRepository;
    @Autowired private SupplierPaymentRepository supplierPaymentRepository;
    @Autowired private PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    @Autowired private BusinessPartyRepository businessPartyRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private TenantApplicationRepository tenantApplicationRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private com.bemo.hr.finance.infrastructure.AccountRepository accountRepository;
    @Autowired private com.bemo.hr.finance.domain.posting.PostingProfileRepository postingProfileRepository;
    @Autowired private com.bemo.hr.finance.domain.posting.PostingProfileLineRepository postingProfileLineRepository;

    private BusinessParty supplier;
    private SupplierInvoice firstInvoice;
    private SupplierInvoice secondInvoice;
    private String proposalId;

    @BeforeEach
    void setUp() {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow();
        TenantContext.set(app.getId());
        LocalDate today = LocalDate.now();
        if (fiscalPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                today, today, List.of(FiscalPeriod.Status.OPEN)).isEmpty()) {
            fiscalPeriodRepository.save(new FiscalPeriod(today.getYear(), today.getMonthValue(),
                    today.getMonth().name(), today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()),
                    FiscalPeriod.Status.OPEN));
        }
        String suffix = Long.toString(System.nanoTime());
        var ap = accountRepository.save(new com.bemo.hr.finance.domain.Account("AP" + suffix, "AP Payable", com.bemo.hr.finance.domain.Account.Type.LIABILITY, null, false, "EGP", true));
        var bank = accountRepository.save(new com.bemo.hr.finance.domain.Account("BANK" + suffix, "Bank Account", com.bemo.hr.finance.domain.Account.Type.ASSET, null, false, "EGP", true));
        var profile = postingProfileRepository.save(new com.bemo.hr.finance.domain.posting.PostingProfile("PROP-PMT-" + suffix, "SUPPLIER_PAYMENT_BANK_TRANSFER", today.minusDays(10), null));
        postingProfileLineRepository.save(new com.bemo.hr.finance.domain.posting.PostingProfileLine(profile.getId(), 1, "DEBIT", "FIXED", ap.getId(), "AMOUNT"));
        postingProfileLineRepository.save(new com.bemo.hr.finance.domain.posting.PostingProfileLine(profile.getId(), 2, "CREDIT", "FIXED", bank.getId(), "AMOUNT"));

        supplier = businessPartyRepository.save(new BusinessParty(
                "PROP-" + suffix, "Proposal Supplier", null, "SUPPLIER", null, null, null, null, null, true,
                "DIRECT", null, null, null, "EGP", "E_INVOICE", "CASH", null,
                "EG123456789012345678901234"));
        firstInvoice = saveInvoice("INV-A-" + suffix, "100.00");
        secondInvoice = saveInvoice("INV-B-" + suffix, "80.00");
    }

    @AfterEach
    void clearTenant() {
        try {
            if (proposalId != null) {
                auditLogRepository.search("VENDOR_PAYMENT_PROPOSAL", null, null, proposalId,
                        null, null, PageRequest.of(0, 20)).forEach(auditLogRepository::delete);
                allocationRepository.findByProposalIdOrderByLineNoAsc(proposalId).forEach(allocationRepository::delete);
                proposalRepository.deleteById(proposalId);
            }
            if (supplier != null) {
                partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(supplier.getId())
                        .forEach(partnerLedgerEntryRepository::delete);
            }
            if (firstInvoice != null) {
                supplierPaymentRepository.findBySupplierInvoiceId(firstInvoice.getId()).forEach(supplierPaymentRepository::delete);
                supplierInvoiceRepository.deleteById(firstInvoice.getId());
            }
            if (secondInvoice != null) {
                supplierPaymentRepository.findBySupplierInvoiceId(secondInvoice.getId()).forEach(supplierPaymentRepository::delete);
                supplierInvoiceRepository.deleteById(secondInvoice.getId());
            }
            if (supplier != null) businessPartyRepository.deleteById(supplier.getId());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void multiInvoiceProposalPersistsCompletePaymentLedgerAuditAndReplayEvidence() {
        var created = vendorPaymentProposalService.createProposal(supplier.getId(), List.of(
                new VendorPaymentProposalService.AllocationInput(firstInvoice.getId(), new BigDecimal("60.00")),
                new VendorPaymentProposalService.AllocationInput(secondInvoice.getId(), new BigDecimal("40.00"))),
                LocalDate.now(), "maker");
        proposalId = created.id();
        var approved = vendorPaymentProposalService.approveProposal(created.id(), "checker");
        var executed = vendorPaymentProposalService.executeProposal(created.id(), "proposal-op", "BANK_TRANSFER", "disburser");
        var replay = vendorPaymentProposalService.executeProposal(created.id(), "proposal-op", "BANK_TRANSFER", "disburser");

        assertThat(approved.status()).isEqualTo(VendorPaymentProposal.Status.APPROVED);
        assertThat(executed.status()).isEqualTo(VendorPaymentProposal.Status.EXECUTED);
        assertThat(replay.id()).isEqualTo(executed.id());
        assertThat(executed.proposedAmount()).isEqualByComparingTo("100.00");
        assertThat(executed.allocations()).hasSize(2)
                .allSatisfy(allocation -> assertThat(allocation.supplierPaymentId()).isNotBlank());

        assertThat(allocationRepository.findByProposalIdOrderByLineNoAsc(created.id())).hasSize(2);
        assertThat(supplierPaymentRepository.findBySupplierInvoiceId(firstInvoice.getId())).singleElement()
                .satisfies(payment -> assertThat(payment.getAmount()).isEqualByComparingTo("60.00"));
        assertThat(supplierPaymentRepository.findBySupplierInvoiceId(secondInvoice.getId())).singleElement()
                .satisfies(payment -> assertThat(payment.getAmount()).isEqualByComparingTo("40.00"));
        assertThat(supplierInvoiceRepository.findById(firstInvoice.getId()).orElseThrow().getStatus())
                .isEqualTo(SupplierInvoice.Status.PARTIALLY_PAID.name());
        assertThat(supplierInvoiceRepository.findById(secondInvoice.getId()).orElseThrow().getStatus())
                .isEqualTo(SupplierInvoice.Status.PARTIALLY_PAID.name());
        assertThat(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(supplier.getId()))
                .hasSize(2)
                .extracting(entry -> entry.getAmountDelta())
                .containsExactlyInAnyOrder(new BigDecimal("60.00"), new BigDecimal("40.00"));

        var audit = auditLogRepository.search("VENDOR_PAYMENT_PROPOSAL", null, null, created.id(),
                null, null, PageRequest.of(0, 10)).getContent();
        assertThat(audit).extracting(entry -> entry.getAction()).containsExactlyInAnyOrder("CREATE", "APPROVE", "EXECUTE");
        assertThat(audit).extracting(entry -> entry.getUsername()).containsExactlyInAnyOrder("maker", "checker", "disburser");
    }

    private SupplierInvoice saveInvoice(String number, String amount) {
        return supplierInvoiceRepository.save(new SupplierInvoice(number, number, null, "EGP", supplier.getId(),
                null, null, null, LocalDate.now(), new BigDecimal(amount), BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDate.now(), null));
    }
}
