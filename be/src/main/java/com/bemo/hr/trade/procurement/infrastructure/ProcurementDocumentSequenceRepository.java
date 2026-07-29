package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.ProcurementDocumentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcurementDocumentSequenceRepository extends JpaRepository<ProcurementDocumentSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProcurementDocumentSequence> findByDocumentType(String documentType);
}
