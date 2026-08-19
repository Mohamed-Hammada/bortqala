package com.bemo.hr.trade.sales.api;

import com.bemo.hr.shared.security.Roles;
import com.bemo.hr.trade.sales.application.CustomerReceiptBankMatchService;
import com.bemo.hr.trade.sales.domain.CustomerReceiptBankMatch;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/sales/receipts/bank-matches")
public class CustomerReceiptBankMatchController {

    private final CustomerReceiptBankMatchService matchService;

    public CustomerReceiptBankMatchController(CustomerReceiptBankMatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.SALES_MANAGER)
    public CustomerReceiptBankMatch matchReceipt(@RequestBody MatchReceiptPayload payload) {
        return matchService.matchReceipt(payload.receiptId(), payload.bankTransactionId(), payload.matchedAmount());
    }

    @GetMapping("/{receiptId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.SALES_MANAGER + " or " + Roles.VIEWER)
    public CustomerReceiptBankMatch getMatchForReceipt(@PathVariable String receiptId) {
        return matchService.getMatchForReceipt(receiptId);
    }

    public record MatchReceiptPayload(String receiptId, String bankTransactionId, BigDecimal matchedAmount) {
    }
}
