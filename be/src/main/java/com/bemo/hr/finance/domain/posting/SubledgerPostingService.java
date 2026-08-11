package com.bemo.hr.finance.domain.posting;

import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SubledgerPostingService {

    private final PostingProfileRepository postingProfileRepository;
    private final JournalEntryRepository journalEntryRepository;

    public SubledgerPostingService(PostingProfileRepository postingProfileRepository,
                                  JournalEntryRepository journalEntryRepository) {
        this.postingProfileRepository = postingProfileRepository;
        this.journalEntryRepository = journalEntryRepository;
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
        if (debitAmount == null || creditAmount == null || debitAmount.compareTo(creditAmount) != 0) {
            throw new BusinessRuleException("Debit and credit amounts must be equal for subledger posting",
                    "SUBLEDGER_UNBALANCED_POSTING", HttpStatus.CONFLICT);
        }

        List<PostingProfile> profiles = postingProfileRepository.findByBusinessEventAndActiveTrue(businessEvent);
        PostingProfile profile = profiles.isEmpty() ? null : profiles.get(0);

        String entryNumber = "POST-" + System.currentTimeMillis();
        JournalEntry journalEntry = new JournalEntry(
                entryNumber,
                eventDate,
                description,
                sourceModule + ":" + sourceDocumentType + ":" + sourceDocumentId,
                fiscalPeriodId
        );
        journalEntry.setOperationId(operationId);
        journalEntry.post("SYSTEM");

        return journalEntryRepository.save(journalEntry);
    }
}
