package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.domain.RfqHeader;
import com.bemo.hr.trade.procurement.domain.SourcingAward;
import com.bemo.hr.trade.procurement.domain.SupplierQuoteHeader;
import com.bemo.hr.trade.procurement.infrastructure.RfqHeaderRepository;
import com.bemo.hr.trade.procurement.infrastructure.SourcingAwardRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierQuoteHeaderRepository;
import com.bemo.hr.trade.procurement.infrastructure.SupplierQuoteLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SourcingServiceTests {

    private RfqHeaderRepository rfqHeaderRepository;
    private SupplierQuoteHeaderRepository quoteHeaderRepository;
    private SupplierQuoteLineRepository quoteLineRepository;
    private SourcingAwardRepository awardRepository;
    private ProcurementService procurementService;
    private SourcingService sourcingService;

    @BeforeEach
    void setUp() {
        rfqHeaderRepository = mock(RfqHeaderRepository.class);
        quoteHeaderRepository = mock(SupplierQuoteHeaderRepository.class);
        quoteLineRepository = mock(SupplierQuoteLineRepository.class);
        awardRepository = mock(SourcingAwardRepository.class);
        procurementService = mock(ProcurementService.class);
        sourcingService = new SourcingService(rfqHeaderRepository, quoteHeaderRepository, quoteLineRepository, awardRepository, procurementService);
    }

    @Test
    void createsRfqAndSubmitsQuoteAndAwardsSuccessfully() {
        RfqHeader rfq = new RfqHeader("RFQ-001", "req-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15));
        rfq.issue();

        SupplierQuoteHeader quote = new SupplierQuoteHeader("rfq-1", "supp-10", "Q-100", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 1), new BigDecimal("10000.00"));

        when(rfqHeaderRepository.findById("rfq-1")).thenReturn(Optional.of(rfq));
        when(quoteHeaderRepository.findById("quote-1")).thenReturn(Optional.of(quote));
        when(rfqHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(quoteHeaderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(awardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcurementApi.PurchaseOrderResponse mockPoResponse = new ProcurementApi.PurchaseOrderResponse(
                "po-99", "PO-99", 0, "supp-10", "Supplier 10", null, null, null, "EGP", "EGP", BigDecimal.ONE, 0, "DEFAULT", null, new BigDecimal("10000.00"), "ISSUED", new BigDecimal("10000.00"), List.of(), 0, 0
        );
        when(procurementService.create(any())).thenReturn(mockPoResponse);

        SourcingAward award = sourcingService.awardQuote("rfq-1", "quote-1", "purchaser1");

        assertThat(award).isNotNull();
        assertThat(award.getSupplierId()).isEqualTo("supp-10");
        assertThat(award.getPurchaseOrderId()).isEqualTo("po-99");
        assertThat(rfq.getStatus()).isEqualTo(RfqHeader.Status.AWARDED);
        assertThat(quote.getStatus()).isEqualTo(SupplierQuoteHeader.Status.AWARDED);
    }
}
