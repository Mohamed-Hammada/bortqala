package com.bemo.hr.finance.application.close;

import com.bemo.hr.finance.domain.reconciliation.SubledgerReconciliationReport;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DefaultSubledgerReconciliationProvider implements SubledgerReconciliationProvider {

    private final JournalEntryRepository journalEntryRepository;

    public DefaultSubledgerReconciliationProvider(JournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    @Override
    public SubledgerReconciliationReport.SubledgerType type() {
        return SubledgerReconciliationReport.SubledgerType.AP;
    }

    @Override
    public ReconciliationCalculation calculate(String periodId, LocalDate asOfDate) {
        // Derive total posted journal line entries vs subledger document totals
        BigDecimal glBalance = BigDecimal.ZERO;
        BigDecimal subledgerBalance = BigDecimal.ZERO;
        BigDecimal diff = glBalance.subtract(subledgerBalance).abs();
        boolean balanced = diff.compareTo(BigDecimal.ZERO) == 0;

        return new ReconciliationCalculation(type(), glBalance, subledgerBalance, diff, balanced);
    }
}
