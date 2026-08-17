package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TrialBalanceReportService {

    private final AccountRepository accountRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final JournalEntryRepository journalEntryRepository;

    public TrialBalanceReportService(AccountRepository accountRepository,
                                     JournalEntryLineRepository journalEntryLineRepository) {
        this(accountRepository, journalEntryLineRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TrialBalanceReportService(AccountRepository accountRepository,
                                     JournalEntryLineRepository journalEntryLineRepository,
                                     JournalEntryRepository journalEntryRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    public List<TrialBalanceRow> generateTrialBalance() {
        return generateTrialBalance(LocalDate.MIN, LocalDate.MAX);
    }

    public List<TrialBalanceRow> generateTrialBalance(LocalDate from, LocalDate to) {
        List<Account> accounts = accountRepository.findAll();
        List<JournalEntryLine> lines;
        if (journalEntryRepository == null) lines = journalEntryLineRepository.findAll();
        else {
            var ids = journalEntryRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED).stream()
                    .filter(e -> !e.getEntryDate().isBefore(from) && !e.getEntryDate().isAfter(to)).map(JournalEntry::getId).toList();
            lines = ids.isEmpty() ? List.of() : journalEntryLineRepository.findByJournalEntryIdIn(ids);
        }

        Map<String, List<JournalEntryLine>> linesByAccount = lines.stream()
                .collect(Collectors.groupingBy(JournalEntryLine::getAccountId));

        List<TrialBalanceRow> rows = new ArrayList<>();
        for (Account account : accounts) {
            List<JournalEntryLine> accountLines = linesByAccount.getOrDefault(account.getId(), List.of());
            BigDecimal totalDebit = accountLines.stream().map(JournalEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredit = accountLines.stream().map(JournalEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal closing = totalDebit.subtract(totalCredit);
            rows.add(new TrialBalanceRow(account.getId(), account.getCode(), account.getName(), totalDebit, totalCredit, closing));
        }
        return rows;
    }

    public record TrialBalanceRow(
            String accountId, String accountCode, String accountName,
            BigDecimal periodDebit, BigDecimal periodCredit, BigDecimal closingBalance
    ) {
    }
}
