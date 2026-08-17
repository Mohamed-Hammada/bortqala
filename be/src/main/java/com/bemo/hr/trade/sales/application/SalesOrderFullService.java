package com.bemo.hr.trade.sales.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.operations.domain.StockReservation;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.api.SalesApi;
import com.bemo.hr.trade.sales.domain.*;
import com.bemo.hr.trade.sales.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesOrderFullService {
    private static final String SOURCE_TYPE = "SALES_ORDER";

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderLineRepository salesOrderLineRepository;
    private final SalesPricingSnapshotRepository pricingSnapshotRepository;
    private final SalesDeliveryHeaderRepository deliveryHeaderRepository;
    private final SalesDeliveryLineRepository deliveryLineRepository;
    private final CustomerReturnHeaderRepository returnHeaderRepository;
    private final CustomerReturnLineRepository returnLineRepository;
    private final CustomerCreditNoteRepository creditNoteRepository;
    private final CustomerInvoiceRepository invoiceRepository;
    private final WarehouseInventoryService warehouseInventoryService;
    private final OperationsService operationsService;
    private final SalesReceivablesService receivablesService;
    private final AuditService auditService;

    private static LocalDate date(long value) {
        return Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static long ms(LocalDate value) {
        return value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private static BusinessRuleException conflict(String code) {
        return new BusinessRuleException(code, code, HttpStatus.CONFLICT);
    }

    private static BusinessRuleException notFound(String code) {
        return new BusinessRuleException(code, code, HttpStatus.NOT_FOUND);
    }

    @Transactional(readOnly = true)
    public List<SalesApi.SalesOrderResponse> orders() {
        return salesOrderRepository.findAllByOrderBySoDateDescCreatedAtDesc().stream().map(this::order).toList();
    }

    @Transactional
    public SalesApi.SalesOrderResponse createOrder(SalesApi.SalesOrderPayload request, String actor) {
        if (salesOrderRepository.existsBySoNumberIgnoreCase(request.soNumber()))
            throw conflict("SALE_ORDER_NUMBER_EXISTS");
        List<SalesApi.SalesOrderLineRequest> lines = request.lines() == null ? List.of() : request.lines();
        if (lines.isEmpty()) throw conflict("O2C_ORDER_LINES_REQUIRED");
        if (request.warehouseId() == null || request.warehouseId().isBlank()) throw conflict("O2C_WAREHOUSE_REQUIRED");
        if (lines.stream().map(SalesApi.SalesOrderLineRequest::itemId).distinct().count() != lines.size()) {
            throw conflict("O2C_ORDER_ITEM_DUPLICATE");
        }
        LocalDate date = Instant.ofEpochMilli(request.soDate()).atZone(ZoneOffset.UTC).toLocalDate();
        SalesOrder salesOrder = new SalesOrder(request.soNumber(), date, request.customerId(), request.quotationId(), BigDecimal.ZERO);
        salesOrder.configureFulfillment(request.warehouseId(), request.currencyCode());
        salesOrder = salesOrderRepository.save(salesOrder);
        BigDecimal total = BigDecimal.ZERO;
        for (SalesApi.SalesOrderLineRequest row : lines) {
            SalesOrderLine line = new SalesOrderLine(salesOrder.getId(), row.itemId(), row.itemName(), row.quantity(), row.unitPrice(), row.discountRate());
            total = total.add(line.getLineTotal());
            salesOrderLineRepository.save(line);
        }
        salesOrder.replaceDerivedTotal(total);
        salesOrderRepository.save(salesOrder);
        auditService.record("CREATE", "SALES_ORDER", salesOrder.getId(), actor,
                "{\"number\":\"" + salesOrder.getSoNumber() + "\",\"amount\":" + total + "}", null);
        return order(salesOrder);
    }

    @Transactional
    public SalesApi.SalesOrderResponse confirmOrder(String id, String actor) {
        SalesOrder salesOrder = lockOrder(id);
        if (salesOrder.getStatus() == SalesOrder.Status.CONFIRMED) return order(salesOrder);
        if (salesOrder.getStatus() != SalesOrder.Status.DRAFT) throw conflict("SALE_ORDER_STATE_INVALID");
        List<SalesOrderLine> lines = salesOrderLineRepository.findBySalesOrderId(id);
        if (lines.isEmpty()) throw conflict("O2C_ORDER_LINES_REQUIRED");
        receivablesService.assertCreditAvailable(salesOrder.getCustomerId(), salesOrder.getTotalAmount());
        for (SalesOrderLine line : lines) {
            if (!pricingSnapshotRepository.existsBySalesOrderIdAndItemId(id, line.getItemId())) {
                pricingSnapshotRepository.save(new SalesPricingSnapshot(id, line.getItemId(), line.getUnitPrice(),
                        line.getDiscountRate(), line.getNetPrice()));
            }
            warehouseInventoryService.reserveStock("RSV-" + salesOrder.getId().substring(0, 8) + "-" + line.getId().substring(0, 8), SOURCE_TYPE,
                    id, line.getItemId(), salesOrder.getWarehouseId(), line.getOrderedQuantity());
        }
        salesOrder.confirm();
        auditService.record("CONFIRM", "SALES_ORDER", id, actor,
                "{\"reservations\":" + lines.size() + "}", null);
        return order(salesOrderRepository.save(salesOrder));
    }

    @Transactional
    public SalesApi.SalesOrderResponse cancelOrder(String id, String actor) {
        SalesOrder salesOrder = lockOrder(id);
        if (salesOrder.getStatus() == SalesOrder.Status.CANCELLED) return order(salesOrder);
        if (salesOrder.getStatus() == SalesOrder.Status.DELIVERED) throw conflict("O2C_DELIVERED_ORDER_CANNOT_CANCEL");
        warehouseInventoryService.reservationsForSource(SOURCE_TYPE, id).stream()
                .filter(row -> row.getStatus() == StockReservation.Status.ACTIVE)
                .forEach(row -> warehouseInventoryService.cancelReservation(row.getId()));
        salesOrder.updateStatus(SalesOrder.Status.CANCELLED);
        auditService.record("CANCEL", "SALES_ORDER", id, actor, "{\"reservationsReleased\":true}", null);
        return order(salesOrderRepository.save(salesOrder));
    }

    @Transactional
    public SalesApi.DeliveryResponse deliver(String salesOrderId, SalesApi.DeliveryRequest request, String actor) {
        SalesDeliveryHeader replay = deliveryHeaderRepository.findByOperationId(request.operationId()).orElse(null);
        if (replay != null) return delivery(replay);
        if (deliveryHeaderRepository.existsByDeliveryNumberIgnoreCase(request.deliveryNumber()))
            throw conflict("O2C_DELIVERY_NUMBER_EXISTS");
        SalesOrder order = lockOrder(salesOrderId);
        if (order.getStatus() != SalesOrder.Status.CONFIRMED) throw conflict("O2C_ORDER_NOT_DELIVERABLE");
        List<SalesOrderLine> orderLines = salesOrderLineRepository.findBySalesOrderId(salesOrderId);
        Map<String, StockReservation> reservations = warehouseInventoryService.reservationsForSource(SOURCE_TYPE, salesOrderId)
                .stream().collect(Collectors.toMap(StockReservation::getItemId, Function.identity()));
        if (reservations.size() != orderLines.size()) throw conflict("O2C_RESERVATION_MISMATCH");
        LocalDate deliveryDate = date(request.deliveryDate());
        SalesDeliveryHeader header = deliveryHeaderRepository.save(new SalesDeliveryHeader(request.deliveryNumber(), salesOrderId,
                order.getCustomerId(), deliveryDate, order.getWarehouseId(), request.operationId()));
        header.ship();
        BigDecimal invoiceAmount = BigDecimal.ZERO;
        for (SalesOrderLine line : orderLines) {
            BigDecimal quantity = line.getOrderedQuantity().subtract(line.getDeliveredQuantity());
            if (quantity.signum() <= 0) continue;
            OperationsService.ValuedMovement movement = operationsService.recordSalesDelivery(line.getItemId(), order.getCustomerId(),
                    order.getWarehouseId(), reservations.get(line.getItemId()).getId(), quantity, request.deliveryNumber(),
                    deliveryDate.atStartOfDay(ZoneOffset.UTC).toInstant(), actor);
            deliveryLineRepository.save(new SalesDeliveryLine(header.getId(), line.getId(), line.getItemId(), quantity,
                    line.getNetPrice(), movement.movementId(), movement.unitCost(), movement.totalCost()));
            line.recordDelivery(quantity);
            salesOrderLineRepository.save(line);
            invoiceAmount = invoiceAmount.add(quantity.multiply(line.getNetPrice()));
        }
        if (invoiceAmount.signum() <= 0) throw conflict("O2C_DELIVERY_LINES_REQUIRED");
        CustomerInvoice invoice = receivablesService.createAndIssueDeliveryInvoice("INV-" + request.deliveryNumber(),
                order.getCustomerId(), order.getId(), deliveryDate, order.getCurrencyCode(), invoiceAmount, actor);
        header.linkInvoice(invoice.getId());
        header.deliver();
        order.deliver();
        deliveryHeaderRepository.save(header);
        salesOrderRepository.save(order);
        auditService.record("DELIVER", "SALES_ORDER", order.getId(), actor,
                "{\"deliveryId\":\"" + header.getId() + "\",\"invoiceId\":\"" + invoice.getId() + "\"}", null);
        return delivery(header);
    }

    @Transactional
    public SalesApi.ReturnResponse receiveReturn(String salesOrderId, SalesApi.ReturnRequest request, String actor) {
        CustomerReturnHeader replay = returnHeaderRepository.findByOperationId(request.operationId()).orElse(null);
        if (replay != null) return customerReturn(replay);
        if (returnHeaderRepository.existsByReturnNumberIgnoreCase(request.returnNumber()))
            throw conflict("O2C_RETURN_NUMBER_EXISTS");
        SalesOrder order = lockOrder(salesOrderId);
        SalesDeliveryHeader delivery = deliveryHeaderRepository.findById(request.deliveryId())
                .orElseThrow(() -> notFound("O2C_DELIVERY_NOT_FOUND"));
        if (!delivery.getSalesOrderId().equals(salesOrderId) || order.getStatus() != SalesOrder.Status.DELIVERED) {
            throw conflict("O2C_RETURN_DELIVERY_MISMATCH");
        }
        Map<String, SalesDeliveryLine> deliveryLines = deliveryLineRepository.findByDeliveryIdOrderByCreatedAtAsc(delivery.getId())
                .stream().collect(Collectors.toMap(SalesDeliveryLine::getId, Function.identity()));
        LocalDate returnDate = date(request.returnDate());
        CustomerReturnHeader header = returnHeaderRepository.save(new CustomerReturnHeader(request.returnNumber(), salesOrderId,
                order.getCustomerId(), returnDate, request.reason(), delivery.getId(), delivery.getWarehouseId(), request.operationId()));
        BigDecimal credit = BigDecimal.ZERO;
        for (SalesApi.ReturnLineRequest row : request.lines()) {
            SalesDeliveryLine source = deliveryLines.get(row.deliveryLineId());
            if (source == null) throw conflict("O2C_RETURN_LINE_INVALID");
            BigDecimal alreadyReturned = returnLineRepository.returnedQuantity(source.getId());
            if (alreadyReturned.add(row.quantity()).compareTo(source.getQuantity()) > 0)
                throw conflict("O2C_RETURN_EXCEEDS_DELIVERY");
            OperationsService.ValuedMovement movement = operationsService.recordCustomerReturn(source.getItemId(), order.getCustomerId(),
                    delivery.getWarehouseId(), row.quantity(), source.getUnitCogs(), request.returnNumber(), row.disposition(),
                    returnDate.atStartOfDay(ZoneOffset.UTC).toInstant(), actor);
            BigDecimal lineCredit = row.quantity().multiply(source.getUnitPrice());
            returnLineRepository.save(new CustomerReturnLine(header.getId(), source.getId(), source.getItemId(), row.quantity(),
                    row.disposition(), movement.movementId(), lineCredit, movement.totalCost()));
            credit = credit.add(lineCredit);
        }
        CustomerCreditNote note = receivablesService.applyReturnCredit(request.operationId() + ":credit", "CN-" + request.returnNumber(),
                delivery.getInvoiceId(), order.getId(), delivery.getId(), header.getId(), returnDate, credit, actor);
        header.receive();
        header.approve();
        header.linkCreditNote(note.getId());
        header.refund();
        returnHeaderRepository.save(header);
        auditService.record("RETURN", "SALES_ORDER", order.getId(), actor,
                "{\"returnId\":\"" + header.getId() + "\",\"creditNoteId\":\"" + note.getId() + "\"}", null);
        return customerReturn(header);
    }

    @Transactional(readOnly = true)
    public List<SalesOrderLine> getSalesOrderLines(String id) {
        return salesOrderLineRepository.findBySalesOrderId(id);
    }

    @Transactional(readOnly = true)
    public List<SalesApi.DeliveryResponse> deliveries(String id) {
        return deliveryHeaderRepository.findBySalesOrderId(id).stream().map(this::delivery).toList();
    }

    @Transactional(readOnly = true)
    public List<SalesApi.ReturnResponse> returns(String id) {
        return returnHeaderRepository.findBySalesOrderId(id).stream().map(this::customerReturn).toList();
    }

    private SalesApi.SalesOrderResponse order(SalesOrder value) {
        List<SalesApi.SalesOrderLineResponse> lines = salesOrderLineRepository.findBySalesOrderId(value.getId()).stream().map(row ->
                new SalesApi.SalesOrderLineResponse(row.getId(), row.getItemId(), row.getItemName(), row.getOrderedQuantity(),
                        row.getDeliveredQuantity(), row.getUnitPrice(), row.getDiscountRate(), row.getNetPrice(), row.getLineTotal())).toList();
        return new SalesApi.SalesOrderResponse(value.getId(), value.getSoNumber(), ms(value.getSoDate()), value.getCustomerId(),
                value.getQuotationId(), value.getStatus().name(), value.getTotalAmount(), value.getWarehouseId(), value.getCurrencyCode(),
                lines, value.getCreatedAt(), value.getUpdatedAt());
    }

    private SalesApi.DeliveryResponse delivery(SalesDeliveryHeader value) {
        CustomerInvoice invoice = value.getInvoiceId() == null ? null : invoiceRepository.findById(value.getInvoiceId()).orElse(null);
        List<SalesApi.DeliveryLineResponse> lines = deliveryLineRepository.findByDeliveryIdOrderByCreatedAtAsc(value.getId()).stream().map(row ->
                new SalesApi.DeliveryLineResponse(row.getId(), row.getSalesOrderLineId(), row.getItemId(), row.getQuantity(),
                        row.getUnitPrice(), row.getStockMovementId(), row.getUnitCogs(), row.getCogsAmount())).toList();
        return new SalesApi.DeliveryResponse(value.getId(), value.getDeliveryNumber(), value.getSalesOrderId(), value.getCustomerId(),
                ms(value.getDeliveryDate()), value.getWarehouseId(), value.getOperationId(), value.getInvoiceId(),
                invoice == null ? null : invoice.getInvoiceNumber(), value.getStatus().name(), lines);
    }

    private SalesApi.ReturnResponse customerReturn(CustomerReturnHeader value) {
        CustomerCreditNote note = value.getCreditNoteId() == null ? null : creditNoteRepository.findById(value.getCreditNoteId()).orElse(null);
        List<SalesApi.ReturnLineResponse> lines = returnLineRepository.findByReturnIdOrderByCreatedAtAsc(value.getId()).stream().map(row ->
                new SalesApi.ReturnLineResponse(row.getId(), row.getDeliveryLineId(), row.getItemId(), row.getQuantity(), row.getDisposition(),
                        row.getStockMovementId(), row.getCreditAmount(), row.getCogsAmount())).toList();
        return new SalesApi.ReturnResponse(value.getId(), value.getReturnNumber(), value.getSalesOrderId(), value.getCustomerId(),
                ms(value.getReturnDate()), value.getReason(), value.getDeliveryId(), value.getWarehouseId(), value.getOperationId(),
                value.getCreditNoteId(), note == null ? null : note.getCreditNoteNumber(), value.getStatus().name(), lines);
    }

    private SalesOrder lockOrder(String id) {
        return salesOrderRepository.findByIdForUpdate(id).orElseThrow(() -> notFound("SALE_ORDER_NOT_FOUND"));
    }
}
