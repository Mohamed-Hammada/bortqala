package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TrialBalanceReportService {

    private final AccountRepository accountRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public TrialBalanceReportService(AccountRepository accountRepository,
                                    JournalEntryLineRepository journalEntryLineRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }

    public record TrialBalanceRow(
            String accountId, String accountCode, String accountName,
            BigDecimal periodDebit, BigDecimal periodCredit, BigDecimal closingBalance
    ) {}

    public List<TrialBalanceRow> generateTrialBalance() {
        List<Account> accounts = accountRepository.findAll();
        List<JournalEntryLine> lines = journalEntryLineRepository.findAll();

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
}
