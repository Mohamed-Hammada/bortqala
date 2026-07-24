package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, String> {
    Optional<ImportBatch> findByChecksum(String checksum);
    List<ImportBatch> findAllByOrderByImportedAtDesc();
}
