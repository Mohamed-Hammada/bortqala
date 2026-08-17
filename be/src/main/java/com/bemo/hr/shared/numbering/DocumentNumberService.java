package com.bemo.hr.shared.numbering;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class DocumentNumberService {

    private final DocumentNumberSequenceRepository sequenceRepository;

    public DocumentNumberService(DocumentNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String next(String documentType, String prefix, LocalDate documentDate) {
        log.debug("next called with documentType={}, prefix={}", documentType, prefix);
        int year = documentDate.getYear();
        DocumentNumberSequence sequence = sequenceRepository.findByDocumentTypeAndYear(documentType, year)
                .orElseGet(() -> sequenceRepository.save(new DocumentNumberSequence(documentType, year, 1)));
        long value = sequence.takeNext();
        String number = String.format(java.util.Locale.ROOT, "%s-%d-%05d", prefix, year, value);
        log.info("Generated document number {} for type={}", number, documentType);
        return number;
    }
}
