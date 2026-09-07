package com.bemo.hr.party.application;

import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.party.api.PartyFinancialPositionApi.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyFinancialPositionServiceTests {

    @Mock
    private BusinessPartyRepository businessPartyRepository;
    @Mock
    private PartnerLedgerEntryRepository partnerLedgerEntryRepository;

    private PartyFinancialPositionService service;

    @BeforeEach
    void setUp() {
        service = new PartyFinancialPositionService(businessPartyRepository, partnerLedgerEntryRepository);
    }

    @Test
    @DisplayName("Computes financial position with debits, credits, net balance and aging")
    void testGetFinancialPosition() {
        BusinessParty party = new BusinessParty("CUST-001", "Al-Nour Contracting", "Al-Nour Contracting", "CUSTOMER",
                "Ahmed", "01000000000", "info@alnour.com", "Cairo", null, true,
                "DIRECT", null, "2026-01-01", null, "EGP", "STANDARD", "NET_30", "123-456", null);
        party.updateCreditProfile(BigDecimal.valueOf(100000), false, 30);

        when(businessPartyRepository.findById(party.getId())).thenReturn(Optional.of(party));

        PartnerLedgerEntry e1 = new PartnerLedgerEntry(party.getId(), "INVOICE", BigDecimal.valueOf(60000), "INV-101",
                "Concrete supply", Instant.now().minusSeconds(40 * 86400), "system");
        PartnerLedgerEntry e2 = new PartnerLedgerEntry(party.getId(), "RECEIPT", BigDecimal.valueOf(-20000), "REC-201",
                "Bank transfer", Instant.now().minusSeconds(10 * 86400), "system");

        when(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(party.getId())).thenReturn(List.of(e1, e2));

        PartyFinancialPositionSummary summary = service.getFinancialPosition(party.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.partyCode()).isEqualTo("CUST-001");
        assertThat(summary.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        assertThat(summary.totalCredits()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(summary.netClosingBalance()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(summary.creditStatus()).isEqualTo("NORMAL");
        // e1 is 40 days old (> 30 days terms => 10 days overdue -> bucket 1-30)
        assertThat(summary.aging().bucket1To30()).isEqualByComparingTo(BigDecimal.valueOf(60000));
    }

    @Test
    @DisplayName("Generates party statement with running balance and chronological lines")
    void testGetStatement() {
        BusinessParty party = new BusinessParty("SUPP-001", "Delta Steel", "Delta Steel", "SUPPLIER",
                "Mohamed", "01100000000", "delta@steel.com", "Alexandria", null, true,
                "DIRECT", null, "2026-01-01", null, "EGP", "STANDARD", "NET_30", "789-012", null);

        when(businessPartyRepository.findById(party.getId())).thenReturn(Optional.of(party));

        PartnerLedgerEntry e1 = new PartnerLedgerEntry(party.getId(), "SUPPLIER_INVOICE", BigDecimal.valueOf(50000), "PINV-01",
                "Rebar delivery", Instant.ofEpochMilli(10000000), "system");
        PartnerLedgerEntry e2 = new PartnerLedgerEntry(party.getId(), "SUPPLIER_PAYMENT", BigDecimal.valueOf(-30000), "PMT-01",
                "Cheque payment", Instant.ofEpochMilli(20000000), "system");

        when(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(party.getId())).thenReturn(List.of(e2, e1));

        PartyStatementResponse response = service.getStatement(party.getId(), null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.lines()).hasSize(2);
        assertThat(response.lines().get(0).runningBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(response.lines().get(1).runningBalance()).isEqualByComparingTo(BigDecimal.valueOf(20000));
    }

    @Test
    @DisplayName("asOfDate: a historical cutoff excludes entries (invoices AND payments) that occurred after it")
    void asOfDateExcludesEntriesAfterTheCutoff() {
        BusinessParty party = new BusinessParty("CUST-002", "Giza Trading", null, "CUSTOMER",
                null, null, null, null, null, true, "DIRECT", null, null, null, "EGP", "STANDARD", "NET_30", null, null);
        party.updateCreditProfile(BigDecimal.valueOf(500000), false, 30);
        when(businessPartyRepository.findById(party.getId())).thenReturn(Optional.of(party));

        Instant invoiceDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant paymentDate = invoiceDate.plusSeconds(45L * 86400); // 45 days later, fully settles the invoice
        PartnerLedgerEntry invoice = new PartnerLedgerEntry(party.getId(), "INVOICE", BigDecimal.valueOf(100000),
                "INV-500", "Historical invoice", invoiceDate, "system");
        PartnerLedgerEntry payment = new PartnerLedgerEntry(party.getId(), "RECEIPT", BigDecimal.valueOf(-100000),
                "REC-500", "Later full payment", paymentDate, "system");
        when(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(party.getId()))
                .thenReturn(List.of(payment, invoice));

        long asOfBeforePayment = invoiceDate.plusSeconds(10L * 86400).toEpochMilli(); // 10 days after invoice, before payment
        PartyFinancialPositionSummary historical = service.getFinancialPosition(party.getId(), asOfBeforePayment);

        // The payment (45 days out) is excluded because it happens after the as-of cutoff (10 days out) —
        // the historical balance must still show the invoice as fully outstanding.
        assertThat(historical.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(historical.totalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(historical.netClosingBalance()).isEqualByComparingTo(BigDecimal.valueOf(100000));

        long asOfAfterPayment = invoiceDate.plusSeconds(100L * 86400).toEpochMilli();
        PartyFinancialPositionSummary afterPayment = service.getFinancialPosition(party.getId(), asOfAfterPayment);

        // Once the as-of date moves past the payment, the same party now nets to zero —
        // proving today vs. a historical as-of date genuinely produce different results.
        assertThat(afterPayment.netClosingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(historical.netClosingBalance()).isNotEqualByComparingTo(afterPayment.netClosingBalance());
    }

    @Test
    @DisplayName("asOfDate: aging buckets are computed relative to the requested date, not real time")
    void agingBucketsAreRelativeToTheRequestedAsOfDate() {
        BusinessParty party = new BusinessParty("CUST-003", "Suez Logistics", null, "CUSTOMER",
                null, null, null, null, null, true, "DIRECT", null, null, null, "EGP", "STANDARD", "NET_30", null, null);
        party.updateCreditProfile(BigDecimal.valueOf(500000), false, 30);
        when(businessPartyRepository.findById(party.getId())).thenReturn(Optional.of(party));

        Instant invoiceDate = Instant.parse("2026-02-01T00:00:00Z");
        PartnerLedgerEntry invoice = new PartnerLedgerEntry(party.getId(), "INVOICE", BigDecimal.valueOf(70000),
                "INV-600", "Aging test invoice", invoiceDate, "system");
        when(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(party.getId())).thenReturn(List.of(invoice));

        // At +10 days (within the 30-day terms), the invoice is not yet due.
        long asOf10Days = invoiceDate.plusSeconds(10L * 86400).toEpochMilli();
        PartyFinancialPositionSummary at10Days = service.getFinancialPosition(party.getId(), asOf10Days);
        assertThat(at10Days.aging().currentNotDue()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(at10Days.aging().bucket1To30()).isEqualByComparingTo(BigDecimal.ZERO);

        // At +40 days (10 days past the 30-day terms), the SAME invoice has moved into the 1-30 overdue bucket.
        long asOf40Days = invoiceDate.plusSeconds(40L * 86400).toEpochMilli();
        PartyFinancialPositionSummary at40Days = service.getFinancialPosition(party.getId(), asOf40Days);
        assertThat(at40Days.aging().currentNotDue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(at40Days.aging().bucket1To30()).isEqualByComparingTo(BigDecimal.valueOf(70000));
    }

    @Test
    @DisplayName("getAgingReport threads asOfDate through to the underlying position calculation")
    void agingReportRespectsAsOfDate() {
        BusinessParty party = new BusinessParty("CUST-004", "Aswan Foods", null, "CUSTOMER",
                null, null, null, null, null, true, "DIRECT", null, null, null, "EGP", "STANDARD", "NET_30", null, null);
        party.updateCreditProfile(BigDecimal.valueOf(500000), false, 30);
        when(businessPartyRepository.findAll()).thenReturn(List.of(party));
        when(businessPartyRepository.findById(party.getId())).thenReturn(Optional.of(party));

        Instant invoiceDate = Instant.parse("2026-03-01T00:00:00Z");
        Instant paymentDate = invoiceDate.plusSeconds(45L * 86400);
        PartnerLedgerEntry invoice = new PartnerLedgerEntry(party.getId(), "INVOICE", BigDecimal.valueOf(80000),
                "INV-700", "Report invoice", invoiceDate, "system");
        PartnerLedgerEntry payment = new PartnerLedgerEntry(party.getId(), "RECEIPT", BigDecimal.valueOf(-80000),
                "REC-700", "Report payment", paymentDate, "system");
        when(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(party.getId()))
                .thenReturn(List.of(payment, invoice));

        long asOfBeforePayment = invoiceDate.plusSeconds(10L * 86400).toEpochMilli();
        AgingReportResponse historicalReport = service.getAgingReport("CUSTOMER", asOfBeforePayment);

        assertThat(historicalReport.asOfDate()).isEqualTo(asOfBeforePayment);
        assertThat(historicalReport.rows()).hasSize(1);
        assertThat(historicalReport.rows().get(0).totalBalance()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(historicalReport.totalOutstanding()).isEqualByComparingTo(BigDecimal.valueOf(80000));

        long asOfAfterPayment = invoiceDate.plusSeconds(100L * 86400).toEpochMilli();
        AgingReportResponse currentReport = service.getAgingReport("CUSTOMER", asOfAfterPayment);

        // After the payment date, the party's net closing balance is zero (proving the payment WAS
        // considered — a different, correct result for a different as-of date, so the parameter is
        // no longer ignored). The row still appears because of a separate, pre-existing aging-bucket
        // limitation unrelated to the asOfDate bug fixed here: bucketing only ages individual debit
        // entries and never lets a later credit offset a bucket, so a fully-paid invoice still shows
        // up in a bucket even though it nets to zero. That is out of scope for this remediation (see
        // docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md — only the asOfDate-ignored bug was in scope).
        assertThat(currentReport.rows()).hasSize(1);
        assertThat(currentReport.rows().get(0).totalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(currentReport.totalOutstanding()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calculates subledger to GL control account reconciliation")
    void testGetArApGlReconciliation() {
        BusinessParty p1 = new BusinessParty("C1", "Client A", null, "CUSTOMER", null, null, null, null, null, true, "DIRECT", null, null, null, "EGP", "STANDARD", "NET_30", null, null);
        when(businessPartyRepository.findAll()).thenReturn(List.of(p1));
        when(partnerLedgerEntryRepository.balance(p1.getId())).thenReturn(BigDecimal.valueOf(75000));

        ArApGlReconciliationResponse rec = service.getArApGlReconciliation("CUSTOMER");

        assertThat(rec.isReconciled()).isTrue();
        assertThat(rec.subledgerTotal()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(rec.glControlAccountBalance()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(rec.variance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
