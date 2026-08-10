package com.bemo.hr.trade.sales.api;

import com.bemo.hr.trade.sales.domain.SalesOrder;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderRepository;
import com.bemo.hr.trade.sales.application.SalesReceivablesService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/trade/sales")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'HR_MANAGER')")
public class SalesController {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesReceivablesService receivablesService;

    public SalesController(SalesOrderRepository salesOrderRepository, SalesReceivablesService receivablesService) {
        this.salesOrderRepository = salesOrderRepository;
        this.receivablesService = receivablesService;
    }

    @GetMapping("/orders")
    public List<SalesApi.SalesOrderResponse> listSalesOrders() {
        return salesOrderRepository.findAllByOrderBySoDateDescCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/orders")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER')")
    public SalesApi.SalesOrderResponse createSalesOrder(@Valid @RequestBody SalesApi.SalesOrderPayload payload) {
        LocalDate soDate = Instant.ofEpochMilli(payload.soDate()).atZone(ZoneOffset.UTC).toLocalDate();
        SalesOrder so = new SalesOrder(payload.soNumber(), soDate, payload.customerId(), payload.quotationId(), payload.totalAmount());
        return toResponse(salesOrderRepository.save(so));
    }

    @PostMapping("/orders/{id}/confirm")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER')")
    public SalesApi.SalesOrderResponse confirmSalesOrder(@PathVariable String id) {
        SalesOrder so = salesOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("أمر البيع غير موجود", "SALE_ORDER_NOT_FOUND", HttpStatus.CONFLICT));
        if (so.getStatus() == SalesOrder.Status.CONFIRMED) return toResponse(so);
        if (so.getStatus() != SalesOrder.Status.DRAFT) throw new BusinessRuleException("SALE_ORDER_STATE_INVALID", "SALE_ORDER_STATE_INVALID", HttpStatus.CONFLICT);
        receivablesService.assertCreditAvailable(so.getCustomerId(), so.getTotalAmount());
        so.updateStatus(SalesOrder.Status.CONFIRMED);
        return toResponse(salesOrderRepository.save(so));
    }

    @GetMapping("/receivables/invoices") public List<SalesApi.InvoiceResponse> invoices(){return receivablesService.invoices();}
    @PostMapping("/receivables/invoices") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.InvoiceResponse createInvoice(@Valid @RequestBody SalesApi.InvoiceRequest request,Authentication auth){return receivablesService.createInvoice(request,auth.getName());}
    @PostMapping("/receivables/invoices/{id}/issue") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.InvoiceResponse issueInvoice(@PathVariable String id,Authentication auth){return receivablesService.issueInvoice(id,auth.getName());}
    @GetMapping("/receivables/receipts") public List<SalesApi.ReceiptResponse> receipts(){return receivablesService.receipts();}
    @PostMapping("/receivables/receipts") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.ReceiptResponse receipt(@Valid @RequestBody SalesApi.ReceiptRequest request,Authentication auth){return receivablesService.recordReceipt(request,auth.getName());}
    @GetMapping("/receivables/aging") public SalesApi.AgingResponse aging(@RequestParam(defaultValue="0") long asOf){return receivablesService.aging(asOf);}
    @GetMapping("/customers/{customerId}/credit") public SalesApi.CreditProfileResponse credit(@PathVariable String customerId){return receivablesService.credit(customerId);}
    @PutMapping("/customers/{customerId}/credit") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.CreditProfileResponse updateCredit(@PathVariable String customerId,@Valid @RequestBody SalesApi.CreditProfileRequest request,Authentication auth){return receivablesService.updateCredit(customerId,request,auth.getName());}
    @GetMapping("/receivables/collections") public List<SalesApi.CollectionTaskResponse> collections(@RequestParam(required=false) Long asOf){LocalDate date=asOf==null?LocalDate.now():Instant.ofEpochMilli(asOf).atZone(ZoneOffset.UTC).toLocalDate();return receivablesService.collections(date);}
    @PutMapping("/receivables/collections/{id}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')") public SalesApi.CollectionTaskResponse updateCollection(@PathVariable String id,@Valid @RequestBody SalesApi.CollectionTaskRequest request,Authentication auth){return receivablesService.updateTask(id,request,auth.getName());}

    private SalesApi.SalesOrderResponse toResponse(SalesOrder so) {
        long soDateMs = so.getSoDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new SalesApi.SalesOrderResponse(
                so.getId(), so.getSoNumber(), soDateMs, so.getCustomerId(),
                so.getQuotationId(), so.getStatus().name(),
                so.getTotalAmount(), so.getCreatedAt(), so.getUpdatedAt()
        );
    }
}
