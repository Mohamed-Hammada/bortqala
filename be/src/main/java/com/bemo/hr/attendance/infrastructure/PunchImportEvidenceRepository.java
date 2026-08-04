package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.PunchImportEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PunchImportEvidenceRepository extends JpaRepository<PunchImportEvidence, PunchImportEvidence.Key> {
    List<PunchImportEvidence> findByBatchIdOrderByRowNumber(String batchId);
    List<PunchImportEvidence> findByPunchId(String punchId);

    @Transactional
    void deleteByBatchId(String batchId);

    @Transactional
    void deleteByPunchId(String punchId);
}
