package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.ForeignExchangeEngineService;
import com.bemo.hr.finance.domain.ExchangeRateRecord;
import com.bemo.hr.shared.security.Roles;
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

    @PostMapping("/rates")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER)
    public ExchangeRateRecord setRate(@RequestBody SetRatePayload payload) {
        return fxService.setRate(payload.fromCurrency(), payload.toCurrency(), payload.rate(), LocalDate.parse(payload.effectiveDate()));
    }

    @PostMapping("/calculate")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER + " or " + Roles.VIEWER)
    public ForeignExchangeEngineService.FxCalculationResult calculate(@RequestBody CalculatePayload payload) {
        return fxService.calculateGainLoss(payload.foreignAmount(), payload.transactionRate(), payload.currentRate());
    }

    @PostMapping("/postings")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER)
    public com.bemo.hr.finance.domain.FxPosting post(@RequestBody PostPayload p) {
        return fxService.post(com.bemo.hr.finance.domain.FxPosting.Type.valueOf(p.type()), p.sourceDocumentId(), p.foreignAmount(), p.transactionRate(), p.closingRate(), p.rateSource(), LocalDate.parse(p.effectiveDate()), p.operationId());
    }

    @PostMapping("/postings/{id}/reverse")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_MANAGER)
    public com.bemo.hr.finance.domain.FxPosting reverse(@PathVariable String id, @RequestBody ReversePayload p, org.springframework.security.core.Authentication auth) {
        return fxService.reverse(id, p.operationId(), LocalDate.parse(p.reversalDate()), p.reason(), auth.getName());
    }

    public record SetRatePayload(String fromCurrency, String toCurrency, BigDecimal rate, String effectiveDate) {
    }

    public record CalculatePayload(BigDecimal foreignAmount, BigDecimal transactionRate, BigDecimal currentRate) {
    }

    public record PostPayload(String type, String sourceDocumentId, BigDecimal foreignAmount,
                              BigDecimal transactionRate, BigDecimal closingRate, String rateSource,
                              String effectiveDate, String operationId) {
    }

    public record ReversePayload(String operationId, String reversalDate, String reason) {
    }
}
