package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SubledgerReconciliationProvider {

    SubledgerReconciliationReport.SubledgerType type();

    ReconciliationCalculation calculate(String periodId, LocalDate asOfDate);

    record ReconciliationCalculation(
            SubledgerReconciliationReport.SubledgerType subledgerType,
            BigDecimal glBalance,
            BigDecimal subledgerBalance,
            BigDecimal difference,
            boolean isBalanced
    ) {}
}
