package com.bemo.hr.finance.domain.posting;

import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.finance.infrastructure.JournalSourceMetadataRepository;
import com.bemo.hr.finance.domain.journal.JournalSourceMetadata;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.audit.application.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SubledgerPostingService {

    private final PostingProfileRepository postingProfileRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final FiscalPeriodGuard fiscalPeriodGuard;
    private final AuditService auditService;
    private final JournalSourceMetadataRepository journalSourceMetadataRepository;

    public SubledgerPostingService(PostingProfileRepository postingProfileRepository,
                                  JournalEntryRepository journalEntryRepository,
                                  JournalEntryLineRepository journalEntryLineRepository,
                                  FiscalPeriodGuard fiscalPeriodGuard,
                                  AuditService auditService,
                                  JournalSourceMetadataRepository journalSourceMetadataRepository) {
        this.postingProfileRepository = postingProfileRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.fiscalPeriodGuard = fiscalPeriodGuard;
        this.auditService = auditService;
        this.journalSourceMetadataRepository = journalSourceMetadataRepository;
    }

    @Transactional
    public JournalEntry postSubledgerEvent(
            String sourceModule,
            String sourceDocumentType,
            String sourceDocumentId,
            String businessEvent,
            String operationId,
            LocalDate eventDate,
            String description,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String fiscalPeriodId
    ) {
        JournalEntry replay = journalEntryRepository.findByOperationId(operationId).orElse(null);
        if (replay != null) return replay;
        if (debitAmount == null || creditAmount == null || debitAmount.compareTo(creditAmount) != 0) {
            throw new BusinessRuleException("Debit and credit amounts must be equal for subledger posting",
                    "SUBLEDGER_UNBALANCED_POSTING", HttpStatus.CONFLICT);
        }
        var fiscalPeriod = fiscalPeriodGuard.requireOpen(eventDate);
        if (fiscalPeriodId != null && !fiscalPeriodId.equals(fiscalPeriod.getId())) {
            throw new BusinessRuleException("The selected fiscal period does not cover the event date.",
                    "SUBLEDGER_FISCAL_PERIOD_MISMATCH", HttpStatus.CONFLICT);
        }

        String debitAccountId = "SYSTEM_DEBIT_ACCOUNT";
        String creditAccountId = "SYSTEM_CREDIT_ACCOUNT";

        String entryNumber = "POST-" + System.currentTimeMillis();
        JournalEntry journalEntry = new JournalEntry(
                entryNumber,
                eventDate,
                description,
                sourceModule + ":" + sourceDocumentType + ":" + sourceDocumentId,
                fiscalPeriod.getId()
        );
        journalEntry.setOperationId(operationId);
        journalEntry.setCurrency("EGP");
        journalEntry.assignCreator("SYSTEM_SUBLEDGER");
        journalEntry.approve("SYSTEM_APPROVER");
        journalEntry.post("SYSTEM");

        JournalEntry savedEntry = journalEntryRepository.save(journalEntry);
        journalSourceMetadataRepository.save(new JournalSourceMetadata(savedEntry.getId(), sourceDocumentType, sourceDocumentId));

        JournalEntryLine debitLine = new JournalEntryLine(savedEntry.getId(), debitAccountId, null, debitAmount, BigDecimal.ZERO, description);
        JournalEntryLine creditLine = new JournalEntryLine(savedEntry.getId(), creditAccountId, null, BigDecimal.ZERO, creditAmount, description);

        journalEntryLineRepository.save(debitLine);
        journalEntryLineRepository.save(creditLine);
        auditService.record("SUBLEDGER_POSTED", "JOURNAL_ENTRY", savedEntry.getId(), "SYSTEM",
                "Source " + sourceModule + ":" + sourceDocumentType + ":" + sourceDocumentId + "; operation=" + operationId, null);

        return savedEntry;
    }

    @Transactional
    public JournalEntry reverse(String originalEntryId, String operationId, LocalDate reversalDate, String reason, String actor) {
        JournalEntry replay = journalEntryRepository.findByOperationId(operationId).orElse(null);
        if (replay != null) return replay;
        fiscalPeriodGuard.requireOpen(reversalDate);
        JournalEntry original = journalEntryRepository.findById(originalEntryId)
                .orElseThrow(() -> new BusinessRuleException("Original posting was not found.", "SUBLEDGER_POSTING_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (original.getStatus() != JournalEntry.Status.POSTED) {
            throw new BusinessRuleException("Only a posted subledger entry can be reversed.", "SUBLEDGER_REVERSAL_STATE_INVALID", HttpStatus.CONFLICT);
        }
        var originalLines = journalEntryLineRepository.findByJournalEntryId(originalEntryId);
        JournalEntry reversal = new JournalEntry("REV-" + System.currentTimeMillis(), reversalDate,
                reason, original.getReference(), fiscalPeriodGuard.requireOpen(reversalDate).getId());
        reversal.setCurrency(original.getCurrency());
        reversal.linkReversalOf(original.getId(), operationId);
        JournalEntry saved = journalEntryRepository.save(reversal);
        originalLines.forEach(line -> journalEntryLineRepository.save(new JournalEntryLine(saved.getId(), line.getAccountId(),
                line.getPartyId(), line.getCredit(), line.getDebit(), "Reversal: " + reason)));
        original.markReversed(saved.getId(), reason, actor, original.getOperationId());
        journalEntryRepository.save(original);
        auditService.record("SUBLEDGER_REVERSED", "JOURNAL_ENTRY", original.getId(), actor,
                "Reversal entry " + saved.getId() + "; operation=" + operationId, null);
        return saved;
    }
}
