package com.bemo.hr.trade.sales.api;

import com.bemo.hr.shared.security.Roles;
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

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.SALES_MANAGER)
    public CustomerInvoice createInvoiceFromDelivery(@RequestBody CreateInvoicePayload payload) {
        return invoiceService.createInvoiceFromDelivery(payload.salesOrderId(), payload.deliveredQuantity(), payload.unitPrice(), payload.unitCogs());
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.SALES_MANAGER + " or " + Roles.VIEWER)
    public CustomerInvoice getInvoice(@PathVariable String id) {
        return invoiceService.getInvoice(id);
    }

    @GetMapping("/orders/{salesOrderId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.SALES_MANAGER + " or " + Roles.VIEWER)
    public List<CustomerInvoice> getInvoicesForSalesOrder(@PathVariable String salesOrderId) {
        return invoiceService.getInvoicesForSalesOrder(salesOrderId);
    }

    public record CreateInvoicePayload(String salesOrderId, BigDecimal deliveredQuantity, BigDecimal unitPrice,
                                       BigDecimal unitCogs) {
    }
}
