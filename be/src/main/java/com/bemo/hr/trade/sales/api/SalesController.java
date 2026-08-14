package com.bemo.hr.trade.sales.api;

import com.bemo.hr.trade.sales.application.SalesOrderFullService;
import com.bemo.hr.trade.sales.application.SalesReceivablesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trade/sales")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'HR_MANAGER')")
@RequiredArgsConstructor
public class SalesController {
    private final SalesOrderFullService salesOrderFullService;
    private final SalesReceivablesService receivablesService;

    @GetMapping("/orders") public List<SalesApi.SalesOrderResponse> listSalesOrders(){return salesOrderFullService.orders();}
    @PostMapping("/orders") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')")
    public SalesApi.SalesOrderResponse createSalesOrder(@Valid @RequestBody SalesApi.SalesOrderPayload payload,Authentication auth){return salesOrderFullService.createOrder(payload,auth.getName());}
    @PostMapping("/orders/{id}/confirm") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')")
    public SalesApi.SalesOrderResponse confirmSalesOrder(@PathVariable String id,Authentication auth){return salesOrderFullService.confirmOrder(id,auth.getName());}
    @PostMapping("/orders/{id}/cancel") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')")
    public SalesApi.SalesOrderResponse cancelSalesOrder(@PathVariable String id,Authentication auth){return salesOrderFullService.cancelOrder(id,auth.getName());}

    @GetMapping("/receivables/invoices") public List<SalesApi.InvoiceResponse> invoices(){return receivablesService.invoices();}
    @PostMapping("/receivables/invoices") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.InvoiceResponse createInvoice(@Valid @RequestBody SalesApi.InvoiceRequest request,Authentication auth){return receivablesService.createInvoice(request,auth.getName());}
    @PostMapping("/receivables/invoices/{id}/issue") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.InvoiceResponse issueInvoice(@PathVariable String id,Authentication auth){return receivablesService.issueInvoice(id,auth.getName());}
    @GetMapping("/receivables/receipts") public List<SalesApi.ReceiptResponse> receipts(){return receivablesService.receipts();}
    @PostMapping("/receivables/receipts") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.ReceiptResponse receipt(@Valid @RequestBody SalesApi.ReceiptRequest request,Authentication auth){return receivablesService.recordReceipt(request,auth.getName());}
    @GetMapping("/receivables/aging") public SalesApi.AgingResponse aging(@RequestParam long asOf){return receivablesService.aging(asOf);}
    @GetMapping("/customers/{customerId}/credit") public SalesApi.CreditProfileResponse credit(@PathVariable String customerId){return receivablesService.credit(customerId);}
    @PutMapping("/customers/{customerId}/credit") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.CreditProfileResponse updateCredit(@PathVariable String customerId,@Valid @RequestBody SalesApi.CreditProfileRequest request,Authentication auth){return receivablesService.updateCredit(customerId,request,auth.getName());}
    @GetMapping("/receivables/collections") public List<SalesApi.CollectionTaskResponse> collections(@RequestParam long asOf){LocalDate date=Instant.ofEpochMilli(asOf).atZone(ZoneOffset.UTC).toLocalDate();return receivablesService.collections(date);}
    @PutMapping("/receivables/collections/{id}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.CollectionTaskResponse updateCollection(@PathVariable String id,@Valid @RequestBody SalesApi.CollectionTaskRequest request,Authentication auth){return receivablesService.updateTask(id,request,auth.getName());}
}
