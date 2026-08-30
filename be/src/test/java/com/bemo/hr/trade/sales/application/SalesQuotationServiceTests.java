package com.bemo.hr.trade.sales.application;

import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.trade.sales.api.SalesQuotationApi;
import com.bemo.hr.trade.sales.domain.*;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderLineRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesQuotationLineRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesQuotationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesQuotationServiceTests {

    @Mock
    private SalesQuotationRepository quotationRepository;
    @Mock
    private SalesQuotationLineRepository quotationLineRepository;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private SalesOrderLineRepository salesOrderLineRepository;
    @Mock
    private BusinessPartyRepository businessPartyRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;

    private SalesQuotationService service;

    @BeforeEach
    void setUp() {
        service = new SalesQuotationService(
                quotationRepository,
                quotationLineRepository,
                salesOrderRepository,
                salesOrderLineRepository,
                businessPartyRepository,
                inventoryItemRepository
        );
    }

    @Test
    void createQuotation_calculatesTotalsAndSavesLines() {
        String customerId = "cust-1";
        BusinessParty customer = new BusinessParty("C-1", "شركة النيل", null, "CUSTOMER", null, null, null, null, null, true, "DIRECT", null, null, null, "EGP", "PER_DELIVERY", "NET_30", null, null);

        when(businessPartyRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(quotationRepository.countByQuotationNumberStartingWith(any())).thenReturn(0L);
        when(quotationRepository.save(any(SalesQuotation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quotationLineRepository.save(any(SalesQuotationLine.class))).thenAnswer(inv -> inv.getArgument(0));

        SalesQuotationApi.CreateQuotationRequest request = new SalesQuotationApi.CreateQuotationRequest(
                customerId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 30),
                "تسليم خلال أسبوع",
                List.of(
                        new SalesQuotationApi.QuotationLineItem("item-1", new BigDecimal("10.00"), new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("140.00"), "بند 1")
                )
        );

        SalesQuotationApi.QuotationResponse res = service.createQuotation(request);

        assertThat(res).isNotNull();
        assertThat(res.subtotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(res.totalAmount()).isEqualByComparingTo(new BigDecimal("1140.00"));
        assertThat(res.status()).isEqualTo(QuotationStatus.DRAFT);
    }

    @Test
    void convertToSalesOrder_createsSalesOrderAndMarksQuoteConverted() {
        String quoteId = "quo-1";
        SalesQuotation quote = new SalesQuotation("QUO-2026-001", "cust-1", LocalDate.now(), LocalDate.now().plusDays(30), "Standard terms");
        quote.updateTotals(new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("140.00"), new BigDecimal("1140.00"));

        SalesQuotationLine line = new SalesQuotationLine(quoteId, "item-1", new BigDecimal("10"), new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("140"), new BigDecimal("1140"), "Line 1");

        when(quotationRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quotationLineRepository.findByQuotationId(quoteId)).thenReturn(List.of(line));
        when(salesOrderRepository.countBySoNumberStartingWith(any())).thenReturn(0L);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quotationRepository.save(any(SalesQuotation.class))).thenAnswer(inv -> inv.getArgument(0));

        SalesQuotationApi.QuotationResponse res = service.convertToSalesOrder(quoteId);

        assertThat(res).isNotNull();
        assertThat(res.status()).isEqualTo(QuotationStatus.CONVERTED);
        verify(salesOrderRepository, times(1)).save(any(SalesOrder.class));
        verify(salesOrderLineRepository, times(1)).save(any(SalesOrderLine.class));
    }
}
