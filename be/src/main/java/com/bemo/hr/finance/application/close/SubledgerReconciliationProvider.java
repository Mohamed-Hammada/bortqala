package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SubledgerReconciliationProvider {

    SubledgerReconciliationReport.SubledgerType type();

    ReconciliationCalculation calculate(String periodId, LocalDate asOfDate);

    record ReconciliationCalculation(
            SubledgerReconciliationReport.SubledgerType subledgerType,
            BigDecimal glBalance,
            BigDecimal subledgerBalance,
            BigDecimal difference,
            boolean isBalanced,
            List<SourceDifference> sourceDifferences
    ) {}

    record SourceDifference(String documentId, String documentNumber, BigDecimal subledgerAmount, BigDecimal glAmount) {}
}
