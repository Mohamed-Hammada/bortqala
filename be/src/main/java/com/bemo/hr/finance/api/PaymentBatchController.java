package com.bemo.hr.finance.api;

import com.bemo.hr.finance.domain.treasury.PaymentBatchHeader;
import com.bemo.hr.finance.domain.treasury.PaymentBatchItem;
import com.bemo.hr.finance.domain.treasury.PaymentBatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/treasury/payment-batches")
public class PaymentBatchController {

    private final PaymentBatchService paymentBatchService;

    public PaymentBatchController(PaymentBatchService paymentBatchService) {
        this.paymentBatchService = paymentBatchService;
    }

    public record CreateBatchPayload(String batchNumber, String sourceCategory, BigDecimal totalAmount) {}
    public record AddItemPayload(String documentId, String payeeId, String payeeName, BigDecimal amount, String bankAccount) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TREASURY_MANAGER', 'ACCOUNTANT')")
    public PaymentBatchHeader createBatch(@RequestBody CreateBatchPayload payload) {
        return paymentBatchService.createBatch(payload.batchNumber(), PaymentBatchHeader.SourceCategory.valueOf(payload.sourceCategory()), payload.totalAmount());
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TREASURY_MANAGER', 'ACCOUNTANT')")
    public PaymentBatchItem addItem(@PathVariable String id, @RequestBody AddItemPayload payload) {
        return paymentBatchService.addBatchItem(id, payload.documentId(), payload.payeeId(), payload.payeeName(), payload.amount(), payload.bankAccount());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TREASURY_MANAGER', 'ACCOUNTANT')")
    public PaymentBatchHeader submitBatch(@PathVariable String id) {
        return paymentBatchService.submitBatch(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TREASURY_MANAGER')")
    public PaymentBatchHeader approveBatch(@PathVariable String id) {
        return paymentBatchService.approveBatch(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TREASURY_MANAGER')")
    public PaymentBatchHeader rejectBatch(@PathVariable String id) {
        return paymentBatchService.rejectBatch(id);
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TREASURY_MANAGER')")
    public PaymentBatchHeader disburseBatch(@PathVariable String id) {
        return paymentBatchService.disburseBatch(id);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TREASURY_MANAGER', 'ACCOUNTANT', 'VIEWER')")
    public List<PaymentBatchItem> getItems(@PathVariable String id) {
        return paymentBatchService.getBatchItems(id);
    }
}
