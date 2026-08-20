package com.bemo.hr.finance.api;

import com.bemo.hr.finance.domain.treasury.PaymentBatchHeader;
import com.bemo.hr.finance.domain.treasury.PaymentBatchItem;
import com.bemo.hr.finance.domain.treasury.PaymentBatchService;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_TREASURY_USER)
    public PaymentBatchHeader createBatch(@RequestBody CreateBatchPayload payload, Authentication authentication) {
        return paymentBatchService.createBatch(payload.batchNumber(), PaymentBatchHeader.SourceCategory.valueOf(payload.sourceCategory()), authentication.getName());
    }

    @PostMapping("/{id}/items")
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_TREASURY_USER)
    public PaymentBatchItem addItem(@PathVariable String id, @RequestBody AddItemPayload payload) {
        return paymentBatchService.addBatchItem(id, payload.documentId(), payload.payeeId(), payload.payeeName(), payload.amount(), payload.bankAccount());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_TREASURY_USER)
    public PaymentBatchHeader submitBatch(@PathVariable String id) {
        return paymentBatchService.submitBatch(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public PaymentBatchHeader approveBatch(@PathVariable String id, Authentication authentication) {
        return paymentBatchService.approveBatch(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER)
    public PaymentBatchHeader rejectBatch(@PathVariable String id) {
        return paymentBatchService.rejectBatch(id);
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_TREASURY_USER)
    public PaymentBatchHeader disburseBatch(@PathVariable String id, @RequestBody DisbursePayload payload, Authentication authentication) {
        return paymentBatchService.disburseBatch(id, payload.operationId(), authentication.getName());
    }

    @GetMapping("/{id}/items")
    @PreAuthorize(Roles.ADMIN_ACCOUNTANT_FINANCE_MANAGER_TREASURY_USER_VIEWER)
    public List<PaymentBatchItem> getItems(@PathVariable String id) {
        return paymentBatchService.getBatchItems(id);
    }

    public record CreateBatchPayload(String batchNumber, String sourceCategory) {
    }

    public record DisbursePayload(String operationId) {
    }

    public record AddItemPayload(String documentId, String payeeId, String payeeName, BigDecimal amount,
                                 String bankAccount) {
    }
}
