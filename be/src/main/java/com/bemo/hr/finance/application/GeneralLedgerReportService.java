package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
public class GeneralLedgerReportService {
    private final JournalEntryRepository entries;
    private final JournalEntryLineRepository lines;
    private final AccountRepository accounts;

    public GeneralLedgerReportService(JournalEntryRepository e, JournalEntryLineRepository l, AccountRepository a) {
        entries = e;
        lines = l;
        accounts = a;
    }

    public List<Row> detail(LocalDate from, LocalDate to, String accountId) {
        return detail(from, to, accountId, null, null);
    }

    public List<Row> detail(LocalDate from, LocalDate to, String accountId, String projectId, String costCodeId) {
        log.debug("detail called with from={}, to={}, accountId={}, projectId={}, costCodeId={}", from, to, accountId, projectId, costCodeId);
        Map<String, Account> map = new HashMap<>();
        accounts.findAll().forEach(a -> map.put(a.getId(), a));
        List<Row> out = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (JournalEntry e : entries.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED).stream()
                .filter(x -> !x.getEntryDate().isBefore(from) && !x.getEntryDate().isAfter(to))
                .sorted(Comparator.comparing(JournalEntry::getEntryDate).thenComparing(JournalEntry::getEntryNumber)).toList()) {
            for (JournalEntryLine l : lines.findByJournalEntryId(e.getId())) {
                if (accountId != null && !accountId.isBlank() && !accountId.equals(l.getAccountId())) continue;
                String lineProject = l.getProjectId() != null ? l.getProjectId() : e.getProjectId();
                String lineCostCode = l.getCostCodeId() != null ? l.getCostCodeId() : e.getCostCodeId();
                if (projectId != null && !projectId.isBlank() && !projectId.equals(lineProject)) continue;
                if (costCodeId != null && !costCodeId.isBlank() && !costCodeId.equals(lineCostCode)) continue;
                running = running.add(l.getDebit()).subtract(l.getCredit());
                Account a = map.get(l.getAccountId());
                out.add(new Row(e.getId(), e.getEntryNumber(), e.getEntryDate(), l.getAccountId(), a == null ? l.getAccountId() : a.getCode(),
                        e.getReference(), l.getDebit(), l.getCredit(), running, l.getMemo(), lineProject, lineCostCode));
            }
        }
        return out;
    }

    public byte[] exportCsv(LocalDate from, LocalDate to, String accountId) {
        return exportCsv(from, to, accountId, null, null);
    }

    public byte[] exportCsv(LocalDate from, LocalDate to, String accountId, String projectId, String costCodeId) {
        log.debug("exportCsv called with from={}, to={}, accountId={}, projectId={}, costCodeId={}", from, to, accountId, projectId, costCodeId);
        StringBuilder b = new StringBuilder("entryNumber,entryDate,accountCode,reference,debit,credit,runningBalance,projectId,costCodeId\n");
        detail(from, to, accountId, projectId, costCodeId).forEach(r -> b.append(r.entryNumber()).append(',')
                .append(r.entryDate()).append(',')
                .append(r.accountCode()).append(',')
                .append(r.reference() == null ? "" : r.reference()).append(',')
                .append(r.debit()).append(',')
                .append(r.credit()).append(',')
                .append(r.runningBalance()).append(',')
                .append(r.projectId() == null ? "" : r.projectId()).append(',')
                .append(r.costCodeId() == null ? "" : r.costCodeId()).append('\n'));
        return b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public record Row(String journalId, String entryNumber, LocalDate entryDate, String accountId, String accountCode,
                      String reference, BigDecimal debit, BigDecimal credit, BigDecimal runningBalance,
                      String memo, String projectId, String costCodeId) {
        public Row(String journalId, String entryNumber, LocalDate entryDate, String accountId, String accountCode,
                   String reference, BigDecimal debit, BigDecimal credit, BigDecimal runningBalance) {
            this(journalId, entryNumber, entryDate, accountId, accountCode, reference, debit, credit, runningBalance, null, null, null);
        }
    }
}
