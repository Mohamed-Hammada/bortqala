package com.bemo.hr.workforce;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface WorkforceImportBatchRepository extends JpaRepository<WorkforceImportBatch, String> {
    Optional<WorkforceImportBatch> findByChecksum(String checksum);
    List<WorkforceImportBatch> findAllByOrderByCreatedAtDesc();
}
