package com.bemo.hr.shared.numbering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class DocumentNumberService {

    private final DocumentNumberSequenceRepository sequenceRepository;

    public DocumentNumberService(DocumentNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String next(String documentType, String prefix, LocalDate documentDate) {
        int year = documentDate.getYear();
        DocumentNumberSequence sequence = sequenceRepository.findByDocumentTypeAndYear(documentType, year)
                .orElseGet(() -> sequenceRepository.save(new DocumentNumberSequence(documentType, year, 1)));
        long value = sequence.takeNext();
        return String.format(java.util.Locale.ROOT, "%s-%d-%05d", prefix, year, value);
    }
}
