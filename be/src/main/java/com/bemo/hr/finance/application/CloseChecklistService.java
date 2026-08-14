package com.bemo.hr.finance.application;

import com.bemo.hr.finance.application.close.SubledgerReconciliationProvider;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.infrastructure.FiscalPeriodRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CloseChecklistService {

    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final List<SubledgerReconciliationProvider> reconciliationProviders;

    public CloseChecklistService(FiscalPeriodRepository fiscalPeriodRepository,
                                 JournalEntryRepository journalEntryRepository,
                                 List<SubledgerReconciliationProvider> reconciliationProviders) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.reconciliationProviders = reconciliationProviders;
    }

    public CloseChecklistSummary computePrecheck(String periodId) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessRuleException("Fiscal period not found", "FIN_FISCAL_PERIOD_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<CloseCheckItem> checks = new ArrayList<>();

        // Check 1: Unposted Draft Journals in Period
        long draftJournals = journalEntryRepository.countByFiscalPeriodIdAndStatus(periodId, com.bemo.hr.finance.domain.JournalEntry.Status.DRAFT);
        if (draftJournals > 0) {
            checks.add(new CloseCheckItem(
                    "GL_DRAFT_JOURNALS",
                    "GL",
                    CloseCheckItem.Severity.BLOCKER,
                    draftJournals,
                    BigDecimal.ZERO,
                    String.format("There are %d draft journals in this period that must be posted or deleted.", draftJournals)
            ));
        } else {
            checks.add(new CloseCheckItem(
                    "GL_DRAFT_JOURNALS",
                    "GL",
                    CloseCheckItem.Severity.PASS,
                    0,
                    BigDecimal.ZERO,
                    "No draft journals found in period."
            ));
        }

        // Check 2: Evaluate All Registered Subledger Reconciliation Providers
        boolean subledgerPass = true;
        for (SubledgerReconciliationProvider provider : reconciliationProviders) {
            var calc = provider.calculate(periodId, period.getEndDate());
            BigDecimal absoluteDifference = calc.difference().abs();
            if (absoluteDifference.compareTo(new BigDecimal("10.00")) > 0) {
                subledgerPass = false;
                checks.add(new CloseCheckItem(
                        "SUBLEDGER_RECONCILIATION_" + provider.type().name(),
                        provider.type().name(),
                        CloseCheckItem.Severity.BLOCKER,
                        1,
                        absoluteDifference,
                        String.format("%s subledger absolute imbalance of %s exceeds tolerance.", provider.type().name(), absoluteDifference)
                ));
            }
        }

        if (subledgerPass) {
            checks.add(new CloseCheckItem(
                    "SUBLEDGER_RECONCILIATION",
                    "FINANCE",
                    CloseCheckItem.Severity.PASS,
                    0,
                    BigDecimal.ZERO,
                    "Subledger balances are reconciled with GL control accounts."
            ));
        }

        boolean canClose = checks.stream().noneMatch(c -> c.severity() == CloseCheckItem.Severity.BLOCKER);

        return new CloseChecklistSummary(
                period.getId(),
                period.getPeriodName(),
                canClose,
                checks
        );
    }
}
