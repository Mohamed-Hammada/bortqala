package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.BankDifferencePostingService;
import com.bemo.hr.finance.domain.treasury.BankDifferencePosting;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/treasury/bank/differences")
public class BankDifferencePostingController {

    private final BankDifferencePostingService service;

    public BankDifferencePostingController(BankDifferencePostingService service) {
        this.service = service;
    }

    @PostMapping("/post")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'TREASURY_USER')")
    public BankDifferencePosting postDifference(@RequestBody PostDifferencePayload payload) {
        return service.postDifference(payload.statementLineId(), BankDifferencePosting.DifferenceType.valueOf(payload.differenceType()), payload.amount());
    }

    public record PostDifferencePayload(String statementLineId, String differenceType, BigDecimal amount) {
    }
}
