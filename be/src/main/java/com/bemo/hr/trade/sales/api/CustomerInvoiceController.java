package com.bemo.hr.trade.sales.api;

import com.bemo.hr.trade.sales.application.CustomerInvoiceService;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/sales/invoices")
public class CustomerInvoiceController {

    private final CustomerInvoiceService invoiceService;

    public CustomerInvoiceController(CustomerInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public record CreateInvoicePayload(String salesOrderId, BigDecimal deliveredQuantity, BigDecimal unitPrice, BigDecimal unitCogs) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'FINANCE_MANAGER')")
    public CustomerInvoice createInvoiceFromDelivery(@RequestBody CreateInvoicePayload payload) {
        return invoiceService.createInvoiceFromDelivery(payload.salesOrderId(), payload.deliveredQuantity(), payload.unitPrice(), payload.unitCogs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public CustomerInvoice getInvoice(@PathVariable String id) {
        return invoiceService.getInvoice(id);
    }

    @GetMapping("/orders/{salesOrderId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'FINANCE_MANAGER', 'VIEWER')")
    public List<CustomerInvoice> getInvoicesForSalesOrder(@PathVariable String salesOrderId) {
        return invoiceService.getInvoicesForSalesOrder(salesOrderId);
    }
}
