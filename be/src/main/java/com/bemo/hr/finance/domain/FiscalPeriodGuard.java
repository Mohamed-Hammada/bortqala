package com.bemo.hr.finance.domain;

import java.time.LocalDate;

public interface FiscalPeriodGuard {
    FiscalPeriod requireOpen(LocalDate transactionDate);
    FiscalPeriod requireAdjustment(LocalDate transactionDate);
}
