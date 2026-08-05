package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
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
                        "لا توجد فترة مالية مفتوحة تغطي هذا التاريخ.", "FISCAL_PERIOD_CLOSED", HttpStatus.CONFLICT));
        if (!period.allowsStandardPosting()) {
            throw new BusinessRuleException(
                    "الفترة المالية في حالة إغلاق جزئي ولا تسمح بترحيل معاملات جديدة إلا بالتعديلات المصرح بها.",
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
                        "لا توجد فترة مالية تسمح بتسجيل تعديلات في هذا التاريخ.", "FISCAL_PERIOD_CLOSED", HttpStatus.CONFLICT));
        return period;
    }
}
