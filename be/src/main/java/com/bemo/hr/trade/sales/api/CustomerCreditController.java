package com.bemo.hr.trade.sales.api;

import com.bemo.hr.shared.security.Roles;
import com.bemo.hr.trade.sales.application.CustomerCreditService;
import com.bemo.hr.trade.sales.domain.CustomerCreditProfile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/trade/sales/credit-profiles")
public class CustomerCreditController {

    private final CustomerCreditService creditService;

    public CustomerCreditController(CustomerCreditService creditService) {
        this.creditService = creditService;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_SALES_MANAGER)
    public CustomerCreditProfile setCreditLimit(@RequestBody SetCreditLimitPayload payload) {
        return creditService.setCreditLimit(payload.customerId(), payload.creditLimit());
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize(Roles.ADMIN_FINANCE_MANAGER_SALES_MANAGER_VIEWER)
    public CustomerCreditProfile getCreditProfile(@PathVariable String customerId) {
        return creditService.getCreditProfile(customerId);
    }

    public record SetCreditLimitPayload(String customerId, BigDecimal creditLimit) {
    }
}
