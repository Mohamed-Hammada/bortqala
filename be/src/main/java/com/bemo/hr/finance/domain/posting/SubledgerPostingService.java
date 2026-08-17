package com.bemo.hr.finance.domain.posting;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.domain.journal.JournalSourceMetadata;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.finance.infrastructure.JournalSourceMetadataRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SubledgerPostingService {

    private final PostingProfileRepository postingProfileRepository;
    private final PostingProfileLineRepository postingProfileLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final FiscalPeriodGuard fiscalPeriodGuard;
    private final AuditService auditService;
    private final JournalSourceMetadataRepository journalSourceMetadataRepository;

    public SubledgerPostingService(PostingProfileRepository postingProfileRepository,
                                   PostingProfileLineRepository postingProfileLineRepository,
                                   JournalEntryRepository journalEntryRepository,
                                   JournalEntryLineRepository journalEntryLineRepository,
                                   FiscalPeriodGuard fiscalPeriodGuard,
                                   AuditService auditService,
                                   JournalSourceMetadataRepository journalSourceMetadataRepository) {
        this.postingProfileRepository = postingProfileRepository;
        this.postingProfileLineRepository = postingProfileLineRepository;
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
        return postSubledgerEvent(sourceModule, sourceDocumentType, sourceDocumentId, businessEvent,
                operationId, eventDate, description, debitAmount, creditAmount, fiscalPeriodId,
                null, "EGP", "SYSTEM_SUBLEDGER");
    }

    @Transactional
    public JournalEntry postSubledgerEvent(
            String sourceModule, String sourceDocumentType, String sourceDocumentId, String businessEvent,
            String operationId, LocalDate eventDate, String description,
            BigDecimal debitAmount, BigDecimal creditAmount, String fiscalPeriodId,
            String partyId, String currency, String actor
    ) {
        log.debug("postSubledgerEvent called with sourceModule={}, operationId={}, businessEvent={}", sourceModule, operationId, businessEvent);
        JournalEntry replay = journalEntryRepository.findByOperationId(operationId).orElse(null);
        if (replay != null) {
            log.debug("PostSubledgerEvent replay detected for operationId={}", operationId);
            return replay;
        }
        if (debitAmount == null || creditAmount == null || debitAmount.compareTo(creditAmount) != 0) {
            log.warn("Validation failed: debit and credit amounts must be equal for operationId={}", operationId);
            throw new BusinessRuleException("Debit and credit amounts must be equal for subledger posting",
                    "SUBLEDGER_UNBALANCED_POSTING", HttpStatus.CONFLICT);
        }
        var fiscalPeriod = fiscalPeriodGuard.requireOpen(eventDate);
        if (fiscalPeriodId != null && !fiscalPeriodId.equals(fiscalPeriod.getId())) {
            log.warn("Fiscal period mismatch for operationId={}: expected={}, actual={}", operationId, fiscalPeriodId, fiscalPeriod.getId());
            throw new BusinessRuleException("The selected fiscal period does not cover the event date.",
                    "SUBLEDGER_FISCAL_PERIOD_MISMATCH", HttpStatus.CONFLICT);
        }

        PostingProfile profile = postingProfileRepository.findByBusinessEventAndActiveTrueOrderByEffectiveFromDesc(businessEvent).stream()
                .filter(candidate -> !eventDate.isBefore(candidate.getEffectiveFrom())
                        && (candidate.getEffectiveTo() == null || !eventDate.isAfter(candidate.getEffectiveTo())))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("No effective posting profile found for businessEvent={}", businessEvent);
                    return new BusinessRuleException("No effective posting profile is configured.",
                            "SUBLEDGER_POSTING_PROFILE_REQUIRED", HttpStatus.CONFLICT);
                });
        List<PostingProfileLine> profileLines = postingProfileLineRepository.findByProfileIdOrderByLineNoAsc(profile.getId());
        String debitAccountId = fixedAccount(profileLines, "DEBIT");
        String creditAccountId = fixedAccount(profileLines, "CREDIT");

        String entryNumber = "POST-" + System.currentTimeMillis();
        JournalEntry journalEntry = new JournalEntry(
                entryNumber,
                eventDate,
                description,
                sourceModule + ":" + sourceDocumentType + ":" + sourceDocumentId,
                fiscalPeriod.getId()
        );
        journalEntry.setOperationId(operationId);
        journalEntry.setCurrency(currency == null || currency.isBlank() ? "EGP" : currency);
        journalEntry.assignCreator(actor);
        journalEntry.approve("SYSTEM_APPROVER");
        journalEntry.post("SYSTEM");

        JournalEntry savedEntry = journalEntryRepository.save(journalEntry);
        journalSourceMetadataRepository.save(new JournalSourceMetadata(savedEntry.getId(), sourceDocumentType, sourceDocumentId));

        JournalEntryLine debitLine = new JournalEntryLine(savedEntry.getId(), debitAccountId, partyId, debitAmount, BigDecimal.ZERO, description);
        JournalEntryLine creditLine = new JournalEntryLine(savedEntry.getId(), creditAccountId, partyId, BigDecimal.ZERO, creditAmount, description);

        journalEntryLineRepository.save(debitLine);
        journalEntryLineRepository.save(creditLine);
        auditService.record("SUBLEDGER_POSTED", "JOURNAL_ENTRY", savedEntry.getId(), "SYSTEM",
                "Source " + sourceModule + ":" + sourceDocumentType + ":" + sourceDocumentId + "; operation=" + operationId, null);

        log.info("JournalEntry {} posted successfully for operationId={}", savedEntry.getId(), operationId);
        return savedEntry;
    }

    @Transactional
    public JournalEntry postProfileEvent(
            String sourceModule, String sourceDocumentType, String sourceDocumentId, String businessEvent,
            String operationId, LocalDate eventDate, String description,
            Map<String, BigDecimal> amountSources, String fiscalPeriodId,
            String partyId, String currency, String actor) {
        log.debug("postProfileEvent called with sourceModule={}, operationId={}, businessEvent={}", sourceModule, operationId, businessEvent);
        JournalEntry replay = journalEntryRepository.findByOperationId(operationId).orElse(null);
        if (replay != null) {
            log.debug("PostProfileEvent replay detected for operationId={}", operationId);
            return replay;
        }
        if (amountSources == null || amountSources.isEmpty()) {
            log.warn("Validation failed: posting amounts are required for operationId={}", operationId);
            throw new BusinessRuleException("Posting amounts are required.",
                    "SUBLEDGER_UNBALANCED_POSTING", HttpStatus.CONFLICT);
        }
        var fiscalPeriod = fiscalPeriodGuard.requireOpen(eventDate);
        if (fiscalPeriodId != null && !fiscalPeriodId.equals(fiscalPeriod.getId())) {
            log.warn("Fiscal period mismatch for operationId={}: expected={}, actual={}", operationId, fiscalPeriodId, fiscalPeriod.getId());
            throw new BusinessRuleException("The selected fiscal period does not cover the event date.",
                    "SUBLEDGER_FISCAL_PERIOD_MISMATCH", HttpStatus.CONFLICT);
        }
        PostingProfile profile = postingProfileRepository
                .findByBusinessEventAndActiveTrueOrderByEffectiveFromDesc(businessEvent).stream()
                .filter(candidate -> !eventDate.isBefore(candidate.getEffectiveFrom())
                        && (candidate.getEffectiveTo() == null || !eventDate.isAfter(candidate.getEffectiveTo())))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("No effective posting profile found for businessEvent={}", businessEvent);
                    return new BusinessRuleException("No effective posting profile is configured.",
                            "SUBLEDGER_POSTING_PROFILE_REQUIRED", HttpStatus.CONFLICT);
                });
        List<PostingProfileLine> profileLines = postingProfileLineRepository.findByProfileIdOrderByLineNoAsc(profile.getId());
        if (profileLines.isEmpty()) {
            log.warn("Posting profile {} has no lines", profile.getId());
            throw new BusinessRuleException("The posting profile has no lines.",
                    "SUBLEDGER_POSTING_PROFILE_INVALID", HttpStatus.CONFLICT);
        }
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (PostingProfileLine line : profileLines) {
            if (!"FIXED".equalsIgnoreCase(line.getAccountSource())
                    || line.getFixedAccountId() == null || line.getFixedAccountId().isBlank()) {
                throw new BusinessRuleException("The posting profile account mapping is incomplete.",
                        "SUBLEDGER_POSTING_PROFILE_INVALID", HttpStatus.CONFLICT);
            }
            BigDecimal amount = amountSources.get(line.getAmountSource());
            if (amount == null || amount.signum() < 0) {
                throw new BusinessRuleException("The posting profile amount mapping is incomplete.",
                        "SUBLEDGER_POSTING_PROFILE_INVALID", HttpStatus.CONFLICT);
            }
            if ("DEBIT".equalsIgnoreCase(line.getSide())) debitTotal = debitTotal.add(amount);
            else if ("CREDIT".equalsIgnoreCase(line.getSide())) creditTotal = creditTotal.add(amount);
            else throw new BusinessRuleException("The posting profile side is invalid.",
                        "SUBLEDGER_POSTING_PROFILE_INVALID", HttpStatus.CONFLICT);
        }
        if (debitTotal.signum() <= 0 || debitTotal.compareTo(creditTotal) != 0) {
            throw new BusinessRuleException("Debit and credit amounts must be equal for subledger posting",
                    "SUBLEDGER_UNBALANCED_POSTING", HttpStatus.CONFLICT);
        }
        String safeActor = actor == null || actor.isBlank() ? "SYSTEM_SUBLEDGER" : actor;
        JournalEntry journalEntry = new JournalEntry("POST-" + System.currentTimeMillis(), eventDate, description,
                sourceModule + ":" + sourceDocumentType + ":" + sourceDocumentId, fiscalPeriod.getId());
        journalEntry.setOperationId(operationId);
        journalEntry.setCurrency(currency == null || currency.isBlank() ? "EGP" : currency);
        journalEntry.assignCreator(safeActor);
        journalEntry.approve("SYSTEM_APPROVER");
        journalEntry.post(safeActor);
        JournalEntry savedEntry = journalEntryRepository.save(journalEntry);
        journalSourceMetadataRepository.save(new JournalSourceMetadata(savedEntry.getId(), sourceDocumentType, sourceDocumentId));
        for (PostingProfileLine line : profileLines) {
            BigDecimal amount = amountSources.get(line.getAmountSource());
            if (amount.signum() == 0) continue;
            journalEntryLineRepository.save(new JournalEntryLine(savedEntry.getId(), line.getFixedAccountId(), partyId,
                    "DEBIT".equalsIgnoreCase(line.getSide()) ? amount : BigDecimal.ZERO,
                    "CREDIT".equalsIgnoreCase(line.getSide()) ? amount : BigDecimal.ZERO, description));
        }
        auditService.record("SUBLEDGER_POSTED", "JOURNAL_ENTRY", savedEntry.getId(), safeActor,
                "Source " + sourceModule + ":" + sourceDocumentType + ":" + sourceDocumentId
                        + "; operation=" + operationId, null);
        return savedEntry;
    }

    private String fixedAccount(List<PostingProfileLine> lines, String side) {
        return lines.stream()
                .filter(line -> side.equalsIgnoreCase(line.getSide()))
                .filter(line -> "FIXED".equalsIgnoreCase(line.getAccountSource()))
                .map(PostingProfileLine::getFixedAccountId)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("The posting profile account mapping is incomplete.",
                        "SUBLEDGER_POSTING_PROFILE_INVALID", HttpStatus.CONFLICT));
    }

    @Transactional
    public JournalEntry reverse(String originalEntryId, String operationId, LocalDate reversalDate, String reason, String actor) {
        log.debug("reverse called with originalEntryId={}, operationId={}, actor={}", originalEntryId, operationId, actor);
        JournalEntry replay = journalEntryRepository.findByOperationId(operationId).orElse(null);
        if (replay != null) {
            log.debug("Reverse replay detected for operationId={}", operationId);
            return replay;
        }
        fiscalPeriodGuard.requireOpen(reversalDate);
        JournalEntry original = journalEntryRepository.findById(originalEntryId)
                .orElseThrow(() -> {
                    log.warn("Original posting {} not found for reversal", originalEntryId);
                    return new BusinessRuleException("Original posting was not found.", "SUBLEDGER_POSTING_NOT_FOUND", HttpStatus.NOT_FOUND);
                });
        if (original.getStatus() != JournalEntry.Status.POSTED) {
            log.warn("Reversal rejected: original entry {} is not in POSTED status", originalEntryId);
            throw new BusinessRuleException("Only a posted subledger entry can be reversed.", "SUBLEDGER_REVERSAL_STATE_INVALID", HttpStatus.CONFLICT);
        }
        var originalLines = journalEntryLineRepository.findByJournalEntryId(originalEntryId);
        JournalEntry reversal = new JournalEntry("REV-" + System.currentTimeMillis(), reversalDate,
                reason, original.getReference(), fiscalPeriodGuard.requireOpen(reversalDate).getId());
        reversal.setCurrency(original.getCurrency());
        reversal.linkReversalOf(original.getId(), operationId, actor);
        JournalEntry saved = journalEntryRepository.save(reversal);
        journalSourceMetadataRepository.findByJournalId(originalEntryId).ifPresent(source ->
                journalSourceMetadataRepository.save(new JournalSourceMetadata(
                        saved.getId(), source.getSourceDocumentType(), source.getSourceDocumentId())));
        originalLines.forEach(line -> journalEntryLineRepository.save(new JournalEntryLine(saved.getId(), line.getAccountId(),
                line.getPartyId(), line.getCredit(), line.getDebit(), "Reversal: " + reason)));
        original.markReversed(saved.getId(), reason, actor, original.getOperationId());
        journalEntryRepository.save(original);
        auditService.record("SUBLEDGER_REVERSED", "JOURNAL_ENTRY", original.getId(), actor,
                "Reversal entry " + saved.getId() + "; operation=" + operationId, null);
        log.info("JournalEntry {} reversed originalEntryId={}, operationId={}", saved.getId(), originalEntryId, operationId);
        return saved;
    }
}
