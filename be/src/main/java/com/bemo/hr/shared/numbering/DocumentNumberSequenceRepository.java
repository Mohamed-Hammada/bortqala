package com.bemo.hr.shared.numbering;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentNumberSequenceRepository extends JpaRepository<DocumentNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentNumberSequence> findByDocumentTypeAndYear(String documentType, int year);
}
