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
