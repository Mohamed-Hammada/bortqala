package com.bemo.hr.trade.sales.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.operations.domain.StockReservation;
import com.bemo.hr.trade.sales.api.SalesApi;
import com.bemo.hr.trade.sales.domain.*;
import com.bemo.hr.trade.sales.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesOrderFullServiceTests {
    private SalesOrderRepository orderRepo;
    private SalesOrderLineRepository lineRepo;
    private SalesPricingSnapshotRepository priceRepo;
    private SalesDeliveryHeaderRepository deliveryRepo;
    private SalesDeliveryLineRepository deliveryLineRepo;
    private CustomerReturnHeaderRepository returnRepo;
    private CustomerReturnLineRepository returnLineRepo;
    private CustomerCreditNoteRepository creditRepo;
    private CustomerInvoiceRepository invoiceRepo;
    private WarehouseInventoryService warehouse;
    private OperationsService operations;
    private SalesReceivablesService ar;
    private SalesOrderFullService service;

    private static long ms(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @BeforeEach
    void setup() {
        orderRepo = mock(SalesOrderRepository.class);
        lineRepo = mock(SalesOrderLineRepository.class);
        priceRepo = mock(SalesPricingSnapshotRepository.class);
        deliveryRepo = mock(SalesDeliveryHeaderRepository.class);
        deliveryLineRepo = mock(SalesDeliveryLineRepository.class);
        returnRepo = mock(CustomerReturnHeaderRepository.class);
        returnLineRepo = mock(CustomerReturnLineRepository.class);
        creditRepo = mock(CustomerCreditNoteRepository.class);
        invoiceRepo = mock(CustomerInvoiceRepository.class);
        warehouse = mock(WarehouseInventoryService.class);
        operations = mock(OperationsService.class);
        ar = mock(SalesReceivablesService.class);
        service = new SalesOrderFullService(orderRepo, lineRepo, priceRepo, deliveryRepo, deliveryLineRepo, returnRepo, returnLineRepo,
                creditRepo, invoiceRepo, warehouse, operations, ar, mock(AuditService.class));
        lenient().when(orderRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(lineRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(priceRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(deliveryRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(deliveryLineRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(returnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(returnLineRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createsRealLinesDerivesTotalAndFreezesEveryPriceBeforeReservation() {
        List<SalesOrderLine> saved = new ArrayList<>();
        when(lineRepo.save(any())).thenAnswer(i -> {
            SalesOrderLine l = i.getArgument(0);
            saved.add(l);
            return l;
        });
        SalesApi.SalesOrderResponse created = service.createOrder(new SalesApi.SalesOrderPayload("SO-1", ms(LocalDate.now()), "cust", null, null, "wh", "EGP", List.of(
                new SalesApi.SalesOrderLineRequest("i1", "One", new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("10")),
                new SalesApi.SalesOrderLineRequest("i2", "Two", new BigDecimal("1"), new BigDecimal("50"), BigDecimal.ZERO))), "maker");
        assertThat(created.totalAmount()).isEqualByComparingTo("230");
        SalesOrder order = new SalesOrder("SO-1", LocalDate.now(), "cust", null, new BigDecimal("230"));
        order.configureFulfillment("wh", "EGP");
        when(orderRepo.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(lineRepo.findBySalesOrderId(order.getId())).thenReturn(saved);
        SalesApi.SalesOrderResponse confirmed = service.confirmOrder(order.getId(), "maker");
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        verify(priceRepo, times(2)).save(any());
        verify(warehouse, times(2)).reserveStock(anyString(), eq("SALES_ORDER"), eq(order.getId()), anyString(), eq("wh"), any());
    }

    @Test
    void deliveryConsumesReservationsCreatesValuedLinesAndIssuedInvoiceThenReplays() {
        List<SalesDeliveryLine> savedDeliveryLines = new ArrayList<>();
        when(deliveryLineRepo.save(any())).thenAnswer(i -> {
            SalesDeliveryLine row = i.getArgument(0);
            savedDeliveryLines.add(row);
            return row;
        });
        when(deliveryLineRepo.findByDeliveryIdOrderByCreatedAtAsc(anyString())).thenAnswer(i -> savedDeliveryLines);
        SalesOrder order = new SalesOrder("SO-2", LocalDate.now(), "cust", null, new BigDecimal("100"));
        order.configureFulfillment("wh", "EGP");
        order.confirm();
        SalesOrderLine line = new SalesOrderLine(order.getId(), "item", "Item", BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO);
        StockReservation reservation = new StockReservation("R", "SALES_ORDER", order.getId(), "item", "wh", BigDecimal.ONE);
        when(orderRepo.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(lineRepo.findBySalesOrderId(order.getId())).thenReturn(List.of(line));
        when(warehouse.reservationsForSource("SALES_ORDER", order.getId())).thenReturn(List.of(reservation));
        when(deliveryRepo.findByOperationId("op")).thenReturn(Optional.empty());
        when(operations.recordSalesDelivery(anyString(), anyString(), anyString(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new OperationsService.ValuedMovement("mov", new BigDecimal("60"), new BigDecimal("60"), "journal"));
        CustomerInvoice invoice = new CustomerInvoice("INV-D1", "cust", order.getId(), LocalDate.now(), LocalDate.now().plusDays(30), "EGP", new BigDecimal("100"));
        invoice.issue("maker");
        when(ar.createAndIssueDeliveryInvoice(anyString(), anyString(), anyString(), any(), anyString(), any(), anyString())).thenReturn(invoice);
        SalesApi.DeliveryResponse result = service.deliver(order.getId(), new SalesApi.DeliveryRequest("D1", ms(LocalDate.now()), "op"), "maker");
        assertThat(result.invoiceId()).isEqualTo(invoice.getId());
        assertThat(result.lines()).singleElement().satisfies(row -> assertThat(row.cogsAmount()).isEqualByComparingTo("60"));
        verify(operations).recordSalesDelivery(anyString(), anyString(), anyString(), anyString(), eq(BigDecimal.ONE), eq("D1"), any(), eq("maker"));
    }

    @Test
    void partialReturnUsesOriginalCogsAndCreatesLinkedCreditNote() {
        List<CustomerReturnLine> savedReturnLines = new ArrayList<>();
        when(returnLineRepo.save(any())).thenAnswer(i -> {
            CustomerReturnLine row = i.getArgument(0);
            savedReturnLines.add(row);
            return row;
        });
        when(returnLineRepo.findByReturnIdOrderByCreatedAtAsc(anyString())).thenAnswer(i -> savedReturnLines);
        SalesOrder order = new SalesOrder("SO-3", LocalDate.now(), "cust", null, new BigDecimal("200"));
        order.configureFulfillment("wh", "EGP");
        order.confirm();
        order.deliver();
        SalesDeliveryHeader delivery = new SalesDeliveryHeader("D3", order.getId(), "cust", LocalDate.now(), "wh", "dop");
        delivery.ship();
        delivery.linkInvoice("invoice");
        delivery.deliver();
        SalesDeliveryLine source = new SalesDeliveryLine(delivery.getId(), "ol", "item", new BigDecimal("2"), new BigDecimal("100"), "mov", new BigDecimal("60"), new BigDecimal("120"));
        when(orderRepo.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(deliveryRepo.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(deliveryLineRepo.findByDeliveryIdOrderByCreatedAtAsc(delivery.getId())).thenReturn(List.of(source));
        when(returnRepo.findByOperationId("rop")).thenReturn(Optional.empty());
        when(returnLineRepo.returnedQuantity(source.getId())).thenReturn(BigDecimal.ZERO);
        when(operations.recordCustomerReturn(anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new OperationsService.ValuedMovement("r-mov", new BigDecimal("60"), new BigDecimal("60"), "r-journal"));
        when(ar.applyReturnCredit(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString()))
                .thenAnswer(i -> new CustomerCreditNote(i.getArgument(1), "cust", "invoice", order.getId(), delivery.getId(), i.getArgument(5), i.getArgument(6), "EGP", i.getArgument(7), i.getArgument(0), "maker"));
        SalesApi.ReturnResponse result = service.receiveReturn(order.getId(), new SalesApi.ReturnRequest("RET-1", delivery.getId(), ms(LocalDate.now()), "reason", "rop",
                List.of(new SalesApi.ReturnLineRequest(source.getId(), BigDecimal.ONE, "AVAILABLE"))), "maker");
        assertThat(result.creditNoteId()).isNotBlank();
        verify(operations).recordCustomerReturn(eq("item"), eq("cust"), eq("wh"), eq(BigDecimal.ONE), eq(new BigDecimal("60")), eq("RET-1"), eq("AVAILABLE"), any(), eq("maker"));
        verify(ar).applyReturnCredit(eq("rop:credit"), eq("CN-RET-1"), eq("invoice"), eq(order.getId()), eq(delivery.getId()), anyString(), any(), eq(new BigDecimal("100")), eq("maker"));
    }

    @Test
    void cancelReleasesOnlyActiveReservationsAndReplaysWithoutDoubleRelease() {
        SalesOrder order = new SalesOrder("SO-4", LocalDate.now(), "cust", null, new BigDecimal("100"));
        order.configureFulfillment("wh", "EGP");
        order.confirm();
        StockReservation active = new StockReservation("R", "SALES_ORDER", order.getId(), "item", "wh", BigDecimal.ONE);
        when(orderRepo.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(warehouse.reservationsForSource("SALES_ORDER", order.getId())).thenReturn(List.of(active));
        assertThat(service.cancelOrder(order.getId(), "maker").status()).isEqualTo("CANCELLED");
        assertThat(service.cancelOrder(order.getId(), "maker").status()).isEqualTo("CANCELLED");
        verify(warehouse, times(1)).cancelReservation(active.getId());
    }
}
