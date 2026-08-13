package com.bemo.hr.finance.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.api.BankReconciliationApi;
import com.bemo.hr.finance.domain.*;
import com.bemo.hr.finance.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankReconciliationService {
    private final BankAccountRepository bankAccountRepository;
    private final BankStatementRepository bankStatementRepository;
    private final BankStatementLineRepository bankStatementLineRepository;
    private final BankReconciliationMatchRepository matchRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AccountRepository accountRepository;
    private final FiscalPeriodGuard fiscalPeriodGuard;
    private final DocumentNumberService documentNumberService;
    private final AuditService auditService;

    public List<BankReconciliationApi.StatementResponse> listStatements() {
        return bankStatementRepository.findAllByOrderByPeriodEndDescImportedAtDesc().stream().map(this::statementResponse).toList();
    }

    @Transactional
    public BankReconciliationApi.WorkbenchResponse importCsv(String bankAccountId, String reference,
            BigDecimal opening, BigDecimal closing, MultipartFile file) {
        BankAccount bank = requireBank(bankAccountId);
        if (bank.getAccountId() == null) throw conflict("Bank account must be linked to a GL account.", "BANK_GL_ACCOUNT_REQUIRED");
        if (file == null || file.isEmpty()) throw conflict("Bank statement CSV file is required.", "BANK_STATEMENT_FILE_REQUIRED");
        if (file.getSize() > 10L * 1024 * 1024) throw conflict("Bank statement file cannot exceed 10 MB.", "BANK_STATEMENT_FILE_TOO_LARGE");
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (java.io.IOException ex) { throw conflict("Bank statement file could not be read.", "BANK_STATEMENT_FILE_READ_FAILED"); }
        String hash = sha256(bytes);
        bankStatementRepository.findByBankAccountIdAndFileHash(bankAccountId, hash).ifPresent(s -> {
            throw conflict("This bank statement file was already imported.", "BANK_STATEMENT_DUPLICATE");
        });
        if (reference == null || reference.isBlank()) throw conflict("Statement reference is required.", "BANK_STATEMENT_REFERENCE_REQUIRED");
        if (bankStatementRepository.existsByBankAccountIdAndStatementReference(bankAccountId, reference.strip()))
            throw conflict("Statement reference already exists for this bank account.", "BANK_STATEMENT_REFERENCE_DUPLICATE");
        List<ParsedLine> parsed = parse(bytes);
        BigDecimal movement = parsed.stream().map(ParsedLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (opening.add(movement).subtract(closing).abs().compareTo(new BigDecimal("0.01")) > 0)
            throw conflict("Opening balance plus statement movements must equal closing balance.", "BANK_STATEMENT_BALANCE_MISMATCH");
        LocalDate start = parsed.stream().map(ParsedLine::date).min(LocalDate::compareTo).orElseThrow();
        LocalDate end = parsed.stream().map(ParsedLine::date).max(LocalDate::compareTo).orElseThrow();
        BankStatement statement = bankStatementRepository.save(new BankStatement(bankAccountId, reference, start, end,
                opening, closing, bank.getCurrencyCode(), safeFileName(file), hash, actor()));
        int number = 1;
        for (ParsedLine line : parsed) {
            bankStatementLineRepository.save(new BankStatementLine(statement.getId(), number++, line.date(), line.valueDate(),
                    line.description(), line.reference(), line.amount(), line.balance(), sha256(line.canonical().getBytes(StandardCharsets.UTF_8))));
        }
        auditService.record("IMPORT", "BANK_STATEMENT", statement.getId(), actor(),
                "{\"reference\":\"" + escape(reference) + "\",\"lines\":" + parsed.size() + "}", null);
        return workbench(statement.getId());
    }

    public BankReconciliationApi.WorkbenchResponse workbench(String statementId) {
        BankStatement statement = requireStatement(statementId);
        BankAccount bank = requireBank(statement.getBankAccountId());
        List<BankStatementLine> lines = bankStatementLineRepository.findByStatementIdOrderByLineNumberAsc(statementId);
        return new BankReconciliationApi.WorkbenchResponse(statementResponse(statement), lines.stream()
                .map(line -> lineResponse(line, bank, true)).toList());
    }

    @Transactional
    public BankReconciliationApi.WorkbenchResponse autoMatch(String statementId, BankReconciliationApi.OperationRequest request) {
        if (!matchRepository.findByOperationId(request.operationId()).isEmpty()) return workbench(statementId);
        BankStatement statement = bankStatementRepository.findByIdForUpdate(statementId)
                .orElseThrow(() -> conflict("Bank statement not found.", "BANK_STATEMENT_NOT_FOUND"));
        BankAccount bank = requireBank(statement.getBankAccountId());
        for (BankStatementLine line : bankStatementLineRepository.findByStatementIdOrderByLineNumberAsc(statementId)) {
            if (line.getStatus() != BankStatementLine.Status.UNMATCHED) continue;
            List<Candidate> candidates = candidates(line, bank).stream()
                    .filter(c -> c.availableAmount().compareTo(line.getAmount().abs()) == 0).toList();
            if (candidates.size() == 1) {
                fiscalPeriodGuard.requireOpen(line.getTransactionDate());
                Candidate candidate = candidates.get(0);
                matchRepository.save(new BankReconciliationMatch(line.getId(), candidate.entry().getId(),
                        line.getAmount().abs(), BankReconciliationMatch.Type.EXACT,
                        request.operationId(), actor()));
                line.addMatch(line.getAmount().abs());
            }
        }
        updateStatement(statement);
        auditService.record("AUTO_MATCH", "BANK_STATEMENT", statementId, actor(),
                "{\"operationId\":\"" + escape(request.operationId()) + "\"}", null);
        return workbench(statementId);
    }

    @Transactional
    public BankReconciliationApi.WorkbenchResponse match(String statementId, String lineId,
            BankReconciliationApi.MatchRequest request) {
        if (!matchRepository.findByOperationId(request.operationId()).isEmpty()) return workbench(statementId);
        BankStatement statement = bankStatementRepository.findByIdForUpdate(statementId)
                .orElseThrow(() -> conflict("Bank statement not found.", "BANK_STATEMENT_NOT_FOUND"));
        BankStatementLine line = bankStatementLineRepository.findByIdForUpdate(lineId)
                .filter(l -> l.getStatementId().equals(statementId))
                .orElseThrow(() -> conflict("Bank statement line not found.", "BANK_STATEMENT_LINE_NOT_FOUND"));
        if (line.getStatus() == BankStatementLine.Status.MATCHED || line.getStatus() == BankStatementLine.Status.IGNORED)
            throw conflict("Bank statement line is already closed.", "BANK_STATEMENT_LINE_CLOSED");
        fiscalPeriodGuard.requireOpen(line.getTransactionDate());
        BankAccount bank = requireBank(statement.getBankAccountId());
        BigDecimal total = BigDecimal.ZERO;
        List<BankReconciliationApi.Allocation> allocations = request.allocations() == null ? List.of() : request.allocations();
        for (BankReconciliationApi.Allocation allocation : allocations) {
            JournalEntry entry = requirePostedJournal(allocation.journalEntryId());
            BigDecimal available = availableBankAmount(entry, bank, line.getAmount().signum());
            if (allocation.amount().compareTo(available) > 0) throw conflict("Allocation exceeds the journal bank amount available.", "BANK_MATCH_ALLOCATION_EXCEEDS_AVAILABLE");
            total = total.add(allocation.amount());
            matchRepository.save(new BankReconciliationMatch(lineId, entry.getId(), allocation.amount(),
                    allocation.amount().compareTo(line.getAmount().abs()) == 0 ? BankReconciliationMatch.Type.EXACT : BankReconciliationMatch.Type.PARTIAL,
                    request.operationId(), actor()));
            line.addMatch(allocation.amount());
        }
        BigDecimal fee = request.feeAmount() == null ? BigDecimal.ZERO : request.feeAmount();
        if (fee.signum() > 0) {
            if (line.getAmount().signum() >= 0) throw conflict("Bank fees can only reconcile a debit statement line.", "BANK_FEE_DIRECTION_INVALID");
            JournalEntry feeEntry = createFeeJournal(statement, line, fee, request.feeExpenseAccountId(), request.operationId());
            matchRepository.save(new BankReconciliationMatch(lineId, feeEntry.getId(), fee,
                    BankReconciliationMatch.Type.FEE, request.operationId(), actor()));
            line.addMatch(fee); total = total.add(fee);
        }
        if (total.signum() == 0) throw conflict("At least one allocation or bank fee is required.", "BANK_MATCH_EMPTY");
        if (line.getMatchedAmount().compareTo(line.getAmount().abs()) > 0)
            throw conflict("Matched amount exceeds the bank statement line.", "BANK_MATCH_AMOUNT_EXCEEDED");
        updateStatement(statement);
        auditService.record("MATCH", "BANK_STATEMENT_LINE", lineId, actor(),
                "{\"operationId\":\"" + escape(request.operationId()) + "\",\"amount\":" + total + "}", null);
        return workbench(statementId);
    }

    @Transactional
    public BankReconciliationApi.WorkbenchResponse reverse(String statementId, String matchId,
            BankReconciliationApi.ReverseRequest request) {
        BankStatement statement = bankStatementRepository.findByIdForUpdate(statementId)
                .orElseThrow(() -> conflict("Bank statement not found.", "BANK_STATEMENT_NOT_FOUND"));
        BankReconciliationMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> conflict("Reconciliation match not found.", "BANK_MATCH_NOT_FOUND"));
        BankStatementLine line = bankStatementLineRepository.findByIdForUpdate(match.getStatementLineId())
                .filter(l -> l.getStatementId().equals(statementId))
                .orElseThrow(() -> conflict("Bank statement line not found.", "BANK_STATEMENT_LINE_NOT_FOUND"));
        if (match.getStatus() == BankReconciliationMatch.Status.REVERSED) return workbench(statementId);
        fiscalPeriodGuard.requireOpen(line.getTransactionDate());
        if (match.getMatchType() == BankReconciliationMatch.Type.FEE) reverseFeeJournal(match, request);
        match.reverse(actor(), request.reason()); line.reverseMatch(match.getMatchedAmount()); updateStatement(statement);
        auditService.record("REVERSE_MATCH", "BANK_RECONCILIATION_MATCH", matchId, actor(),
                "{\"reason\":\"" + escape(request.reason()) + "\"}", null);
        return workbench(statementId);
    }

    public BankReconciliationApi.CashPositionResponse cashPosition() {
        List<BankReconciliationApi.CashPositionLine> result = bankAccountRepository.findAllByOrderByBankNameAsc().stream()
                .filter(BankAccount::isActive).map(bank -> {
                    Optional<BankStatement> latest = bankStatementRepository.findFirstByBankAccountIdOrderByPeriodEndDesc(bank.getId());
                    long unmatched = latest.map(s -> bankStatementLineRepository.countByStatementIdAndStatusIn(s.getId(),
                            List.of(BankStatementLine.Status.UNMATCHED, BankStatementLine.Status.PARTIAL))).orElse(0L);
                    return new BankReconciliationApi.CashPositionLine(bank.getId(), bank.getBankName(), bank.getCurrencyCode(),
                            latest.map(BankStatement::getClosingBalance).orElse(BigDecimal.ZERO),
                            latest.map(s -> epoch(s.getPeriodEnd())).orElse(null), unmatched);
                }).toList();
        Map<String, BigDecimal> totals = result.stream().collect(java.util.stream.Collectors.groupingBy(
                BankReconciliationApi.CashPositionLine::currencyCode, java.util.TreeMap::new,
                java.util.stream.Collectors.reducing(BigDecimal.ZERO,
                        BankReconciliationApi.CashPositionLine::latestStatementBalance, BigDecimal::add)));
        return new BankReconciliationApi.CashPositionResponse(result, totals);
    }

    private List<Candidate> candidates(BankStatementLine line, BankAccount bank) {
        if (bank.getAccountId() == null) return List.of();
        return journalEntryRepository.findByStatusOrderByEntryDateDesc(JournalEntry.Status.POSTED).stream()
                .filter(e -> Math.abs(ChronoUnit.DAYS.between(e.getEntryDate(), line.getTransactionDate())) <= 3)
                .map(e -> new Candidate(e, availableBankAmount(e, bank, line.getAmount().signum()), score(e, line)))
                .filter(c -> c.availableAmount().signum() > 0)
                .sorted(Comparator.comparingInt(Candidate::score).reversed()).toList();
    }

    private BigDecimal availableBankAmount(JournalEntry entry, BankAccount bank, int statementSign) {
        BigDecimal effect = journalEntryLineRepository.findByJournalEntryId(entry.getId()).stream()
                .filter(l -> l.getAccountId().equals(bank.getAccountId()))
                .map(l -> l.getDebit().subtract(l.getCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (effect.signum() != statementSign) return BigDecimal.ZERO;
        BigDecimal used = matchRepository.findByJournalEntryIdAndStatus(entry.getId(), BankReconciliationMatch.Status.ACTIVE)
                .stream().map(BankReconciliationMatch::getMatchedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return effect.abs().subtract(used).max(BigDecimal.ZERO);
    }

    private int score(JournalEntry entry, BankStatementLine line) {
        int score = entry.getEntryDate().equals(line.getTransactionDate()) ? 50 : 30;
        String reference = line.getBankReference() == null ? "" : line.getBankReference().toLowerCase();
        if (!reference.isBlank() && ((entry.getReference() != null && entry.getReference().toLowerCase().contains(reference))
                || entry.getDescription().toLowerCase().contains(reference))) score += 30;
        return score;
    }

    private JournalEntry createFeeJournal(BankStatement statement, BankStatementLine line, BigDecimal fee,
            String expenseAccountId, String operationId) {
        BankAccount bank = requireBank(statement.getBankAccountId());
        if (expenseAccountId == null || expenseAccountId.isBlank()) throw conflict("Bank-fee expense account is required.", "BANK_FEE_ACCOUNT_REQUIRED");
        Account expense = accountRepository.findById(expenseAccountId)
                .orElseThrow(() -> conflict("Bank-fee expense account was not found.", "BANK_FEE_ACCOUNT_NOT_FOUND"));
        if (!expense.isActive() || expense.isHeader()) throw conflict("Bank-fee account must be an active posting account.", "BANK_FEE_ACCOUNT_INVALID");
        FiscalPeriod period = fiscalPeriodGuard.requireAdjustment(line.getTransactionDate());
        JournalEntry entry = new JournalEntry(documentNumberService.next("JOURNAL_ENTRY", "JV", line.getTransactionDate()),
                line.getTransactionDate(), "Bank fee — " + line.getDescription(), line.getBankReference(), period.getId());
        entry.setCurrency(statement.getCurrencyCode()); entry.setOperationId(operationId + ":FEE"); entry.assignCreator(actor());
        entry.approve("SYSTEM_APPROVER"); entry.post(actor());
        entry = journalEntryRepository.save(entry);
        journalEntryLineRepository.save(new JournalEntryLine(entry.getId(), expenseAccountId, null, fee, BigDecimal.ZERO, "Bank fee"));
        journalEntryLineRepository.save(new JournalEntryLine(entry.getId(), bank.getAccountId(), null, BigDecimal.ZERO, fee, "Bank fee"));
        return entry;
    }

    private void reverseFeeJournal(BankReconciliationMatch match, BankReconciliationApi.ReverseRequest request) {
        JournalEntry original = requirePostedJournal(match.getJournalEntryId());
        FiscalPeriod period = fiscalPeriodGuard.requireOpen(original.getEntryDate());
        JournalEntry reversal = new JournalEntry(original.getEntryNumber() + "-R", original.getEntryDate(),
                "Reverse bank fee — " + request.reason(), original.getReference(), period.getId());
        reversal.setCurrency(original.getCurrency()); reversal.linkReversalOf(original.getId(), request.operationId());
        reversal = journalEntryRepository.save(reversal);
        for (JournalEntryLine source : journalEntryLineRepository.findByJournalEntryId(original.getId()))
            journalEntryLineRepository.save(new JournalEntryLine(reversal.getId(), source.getAccountId(), source.getPartyId(),
                    source.getCredit(), source.getDebit(), source.getMemo()));
        original.markReversed(reversal.getId(), request.reason(), actor(), request.operationId());
    }

    private void updateStatement(BankStatement statement) {
        long open = bankStatementLineRepository.countByStatementIdAndStatusIn(statement.getId(),
                List.of(BankStatementLine.Status.UNMATCHED, BankStatementLine.Status.PARTIAL));
        statement.updateProgress(open, actor());
    }

    private BankReconciliationApi.LineResponse lineResponse(BankStatementLine line, BankAccount bank, boolean includeCandidates) {
        List<BankReconciliationApi.MatchResponse> matches = matchRepository.findByStatementLineIdOrderByMatchedAtAsc(line.getId()).stream()
                .map(m -> new BankReconciliationApi.MatchResponse(m.getId(), m.getJournalEntryId(), m.getMatchedAmount(),
                        m.getMatchType().name(), m.getStatus().name(), m.getMatchedBy(), m.getMatchedAt().toEpochMilli(),
                        m.getReversedBy(), m.getReversedAt() == null ? null : m.getReversedAt().toEpochMilli(), m.getReversalReason())).toList();
        List<BankReconciliationApi.CandidateResponse> suggestions = includeCandidates && line.getStatus() != BankStatementLine.Status.MATCHED
                ? candidates(line, bank).stream().map(c -> new BankReconciliationApi.CandidateResponse(c.entry().getId(),
                    c.entry().getEntryNumber(), epoch(c.entry().getEntryDate()), c.entry().getDescription(), c.entry().getReference(),
                    signedBankAmount(c.entry(), bank), c.availableAmount(), c.score(), c.score() >= 80 ? "AMOUNT_DATE_REFERENCE" : "AMOUNT_DATE")).toList()
                : List.of();
        return new BankReconciliationApi.LineResponse(line.getId(), line.getLineNumber(), epoch(line.getTransactionDate()),
                line.getValueDate() == null ? null : epoch(line.getValueDate()), line.getDescription(), line.getBankReference(),
                line.getAmount(), line.getRunningBalance(), line.getStatus().name(), line.getMatchedAmount(), line.remainingAmount(),
                line.getVersion(), matches, suggestions);
    }

    private BigDecimal signedBankAmount(JournalEntry entry, BankAccount bank) {
        return journalEntryLineRepository.findByJournalEntryId(entry.getId()).stream().filter(l -> l.getAccountId().equals(bank.getAccountId()))
                .map(l -> l.getDebit().subtract(l.getCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BankReconciliationApi.StatementResponse statementResponse(BankStatement s) {
        List<BankStatementLine> lines = bankStatementLineRepository.findByStatementIdOrderByLineNumberAsc(s.getId());
        long open = lines.stream().filter(l -> l.getStatus() == BankStatementLine.Status.UNMATCHED || l.getStatus() == BankStatementLine.Status.PARTIAL).count();
        return new BankReconciliationApi.StatementResponse(s.getId(), s.getBankAccountId(), s.getStatementReference(),
                epoch(s.getPeriodStart()), epoch(s.getPeriodEnd()), s.getOpeningBalance(), s.getClosingBalance(), s.getCurrencyCode(),
                s.getFileName(), s.getStatus().name(), s.getImportedBy(), s.getImportedAt().toEpochMilli(), s.getReconciledBy(),
                s.getReconciledAt() == null ? null : s.getReconciledAt().toEpochMilli(), s.getVersion(), lines.size(), open);
    }

    private List<ParsedLine> parse(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8).replace("\uFEFF", "");
        List<String> rows = text.lines().filter(s -> !s.isBlank()).toList();
        if (rows.size() < 2) throw conflict("Bank statement CSV has no transaction lines.", "BANK_STATEMENT_EMPTY");
        List<String> header = csvRow(rows.get(0)).stream().map(s -> s.strip().toLowerCase()).toList();
        int date = requiredColumn(header, "date"), description = requiredColumn(header, "description"), amount = requiredColumn(header, "amount");
        int valueDate = optionalColumn(header, "valuedate"), reference = optionalColumn(header, "reference"), balance = optionalColumn(header, "balance");
        List<ParsedLine> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> values = csvRow(rows.get(i));
            try {
                result.add(new ParsedLine(LocalDate.parse(value(values, date)), optionalDate(values, valueDate),
                        value(values, description), optional(values, reference), new BigDecimal(value(values, amount).replace(",", "")),
                        optionalDecimal(values, balance)));
            } catch (RuntimeException ex) { throw conflict("Invalid bank statement CSV row " + (i + 1) + ".", "BANK_STATEMENT_ROW_INVALID"); }
        }
        return result;
    }

    private List<String> csvRow(String row) {
        List<String> values = new ArrayList<>(); StringBuilder value = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < row.length(); i++) { char ch = row.charAt(i);
            if (ch == '"') { if (quoted && i + 1 < row.length() && row.charAt(i + 1) == '"') { value.append('"'); i++; } else quoted = !quoted; }
            else if (ch == ',' && !quoted) { values.add(value.toString().strip()); value.setLength(0); }
            else value.append(ch);
        }
        if (quoted) throw conflict("Unclosed quote in bank statement CSV.", "BANK_STATEMENT_ROW_INVALID");
        values.add(value.toString().strip()); return values;
    }

    private int requiredColumn(List<String> h, String name) { int i = h.indexOf(name); if (i < 0) throw conflict("Missing CSV column: " + name, "BANK_STATEMENT_COLUMN_MISSING"); return i; }
    private int optionalColumn(List<String> h, String name) { return h.indexOf(name); }
    private String value(List<String> v, int i) { if (i < 0 || i >= v.size() || v.get(i).isBlank()) throw new IllegalArgumentException(); return v.get(i); }
    private String optional(List<String> v, int i) { return i < 0 || i >= v.size() || v.get(i).isBlank() ? null : v.get(i); }
    private LocalDate optionalDate(List<String> v, int i) { String s = optional(v, i); return s == null ? null : LocalDate.parse(s); }
    private BigDecimal optionalDecimal(List<String> v, int i) { String s = optional(v, i); return s == null ? null : new BigDecimal(s.replace(",", "")); }
    private JournalEntry requirePostedJournal(String id) { JournalEntry e = journalEntryRepository.findById(id).orElseThrow(() -> conflict("Journal entry not found.", "JOURNAL_NOT_FOUND")); if (e.getStatus() != JournalEntry.Status.POSTED) throw conflict("Only posted journals can be reconciled.", "BANK_MATCH_JOURNAL_NOT_POSTED"); return e; }
    private BankAccount requireBank(String id) { return bankAccountRepository.findById(id).filter(BankAccount::isActive).orElseThrow(() -> conflict("Active bank account not found.", "FIN_BANK_ACCOUNT_NOT_FOUND")); }
    private BankStatement requireStatement(String id) { return bankStatementRepository.findById(id).orElseThrow(() -> conflict("Bank statement not found.", "BANK_STATEMENT_NOT_FOUND")); }
    private String safeFileName(MultipartFile f) { return f.getOriginalFilename() == null ? "statement.csv" : f.getOriginalFilename(); }
    private long epoch(LocalDate date) { return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(); }
    private String actor() { var a = SecurityContextHolder.getContext().getAuthentication(); return a == null ? "system" : a.getName(); }
    private String escape(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private String sha256(byte[] value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); } }
    private BusinessRuleException conflict(String message, String code) { return new BusinessRuleException(message, code, HttpStatus.CONFLICT); }
    private record Candidate(JournalEntry entry, BigDecimal availableAmount, int score) { }
    private record ParsedLine(LocalDate date, LocalDate valueDate, String description, String reference, BigDecimal amount, BigDecimal balance) {
        String canonical() { return date + "|" + valueDate + "|" + description + "|" + reference + "|" + amount + "|" + balance; }
    }
}
