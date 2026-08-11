package com.bemo.hr.trade.sales.api;

import com.bemo.hr.trade.sales.application.SalesOrderFullService;
import com.bemo.hr.trade.sales.domain.CustomerReturnHeader;
import com.bemo.hr.trade.sales.domain.SalesDeliveryHeader;
import com.bemo.hr.trade.sales.domain.SalesOrderLine;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/sales/orders")
public class SalesOrderFullController {

    private final SalesOrderFullService salesOrderFullService;

    public SalesOrderFullController(SalesOrderFullService salesOrderFullService) {
        this.salesOrderFullService = salesOrderFullService;
    }

    public record AddLinePayload(String itemId, String itemName, BigDecimal orderedQuantity, BigDecimal unitPrice, BigDecimal discountRate) {}
    public record CreateDeliveryPayload(String deliveryNumber, String customerId, String deliveryDate) {}
    public record CreateReturnPayload(String returnNumber, String customerId, String returnDate, String reason) {}

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER')")
    public SalesOrderLine addLine(@PathVariable String id, @RequestBody AddLinePayload payload) {
        return salesOrderFullService.addSalesOrderLine(id, payload.itemId(), payload.itemName(), payload.orderedQuantity(), payload.unitPrice(), payload.discountRate());
    }

    @PostMapping("/{id}/deliveries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER')")
    public SalesDeliveryHeader createDelivery(@PathVariable String id, @RequestBody CreateDeliveryPayload payload) {
        return salesOrderFullService.createDelivery(payload.deliveryNumber(), id, payload.customerId(), LocalDate.parse(payload.deliveryDate()));
    }

    @PostMapping("/{id}/returns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER')")
    public CustomerReturnHeader createReturn(@PathVariable String id, @RequestBody CreateReturnPayload payload) {
        return salesOrderFullService.createCustomerReturn(payload.returnNumber(), id, payload.customerId(), LocalDate.parse(payload.returnDate()), payload.reason());
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'VIEWER')")
    public List<SalesOrderLine> getLines(@PathVariable String id) {
        return salesOrderFullService.getSalesOrderLines(id);
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'VIEWER')")
    public List<SalesDeliveryHeader> getDeliveries(@PathVariable String id) {
        return salesOrderFullService.getDeliveriesForOrder(id);
    }

    @GetMapping("/{id}/returns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'VIEWER')")
    public List<CustomerReturnHeader> getReturns(@PathVariable String id) {
        return salesOrderFullService.getReturnsForOrder(id);
    }
}
