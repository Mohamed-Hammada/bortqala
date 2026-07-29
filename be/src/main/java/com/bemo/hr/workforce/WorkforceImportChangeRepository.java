package com.bemo.hr.workforce;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WorkforceImportChangeRepository extends JpaRepository<WorkforceImportChange, String> {
    List<WorkforceImportChange> findByBatchIdOrderByCreatedAtDesc(String batchId);
}
