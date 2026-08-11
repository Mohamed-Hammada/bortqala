package com.bemo.hr.finance.domain.posting;

import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
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

    public SubledgerPostingService(PostingProfileRepository postingProfileRepository,
                                  JournalEntryRepository journalEntryRepository,
                                  JournalEntryLineRepository journalEntryLineRepository) {
        this.postingProfileRepository = postingProfileRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
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

        String debitAccountId = "SYSTEM_DEBIT_ACCOUNT";
        String creditAccountId = "SYSTEM_CREDIT_ACCOUNT";

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

        JournalEntry savedEntry = journalEntryRepository.save(journalEntry);

        JournalEntryLine debitLine = new JournalEntryLine(savedEntry.getId(), debitAccountId, null, debitAmount, BigDecimal.ZERO, description);
        JournalEntryLine creditLine = new JournalEntryLine(savedEntry.getId(), creditAccountId, null, BigDecimal.ZERO, creditAmount, description);

        journalEntryLineRepository.save(debitLine);
        journalEntryLineRepository.save(creditLine);

        return savedEntry;
    }
}
