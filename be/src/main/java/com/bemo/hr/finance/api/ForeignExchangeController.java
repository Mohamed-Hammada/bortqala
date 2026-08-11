package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.ForeignExchangeEngineService;
import com.bemo.hr.finance.domain.ExchangeRateRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finance/fx")
public class ForeignExchangeController {

    private final ForeignExchangeEngineService fxService;

    public ForeignExchangeController(ForeignExchangeEngineService fxService) {
        this.fxService = fxService;
    }

    public record SetRatePayload(String fromCurrency, String toCurrency, BigDecimal rate, String effectiveDate) {}
    public record CalculatePayload(BigDecimal foreignAmount, BigDecimal transactionRate, BigDecimal currentRate) {}

    @PostMapping("/rates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public ExchangeRateRecord setRate(@RequestBody SetRatePayload payload) {
        return fxService.setRate(payload.fromCurrency(), payload.toCurrency(), payload.rate(), LocalDate.parse(payload.effectiveDate()));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'VIEWER')")
    public ForeignExchangeEngineService.FxCalculationResult calculate(@RequestBody CalculatePayload payload) {
        return fxService.calculateGainLoss(payload.foreignAmount(), payload.transactionRate(), payload.currentRate());
    }
}
