package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DefaultFiscalPeriodGuard implements FiscalPeriodGuard {

    private final FiscalPeriodRepository fiscalPeriodRepository;

    public DefaultFiscalPeriodGuard(FiscalPeriodRepository fiscalPeriodRepository) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    @Override
    public FiscalPeriod requireOpen(LocalDate transactionDate) {
        FiscalPeriod period = fiscalPeriodRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                        transactionDate, transactionDate,
                        List.of(FiscalPeriod.Status.OPEN, FiscalPeriod.Status.SOFT_CLOSED))
                .orElseThrow(() -> new BusinessRuleException(
                        "No open fiscal period covers this date.", "FISCAL_PERIOD_CLOSED", HttpStatus.CONFLICT));
        if (!period.allowsStandardPosting()) {
            throw new BusinessRuleException(
                    "Fiscal period is soft-closed and does not allow new postings except for authorized adjustments.",
                    "FISCAL_PERIOD_SOFT_CLOSED", HttpStatus.CONFLICT);
        }
        return period;
    }

    @Override
    public FiscalPeriod requireAdjustment(LocalDate transactionDate) {
        FiscalPeriod period = fiscalPeriodRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
                        transactionDate, transactionDate,
                        List.of(FiscalPeriod.Status.OPEN, FiscalPeriod.Status.SOFT_CLOSED))
                .orElseThrow(() -> new BusinessRuleException(
                        "No fiscal period allows adjustments on this date.", "FISCAL_PERIOD_CLOSED", HttpStatus.CONFLICT));
        return period;
    }
}
