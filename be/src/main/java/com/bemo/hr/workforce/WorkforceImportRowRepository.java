package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkforceImportRowRepository extends JpaRepository<WorkforceImportRow, String> {
    List<WorkforceImportRow> findByBatchIdOrderByRowNumberAsc(String batchId);

    List<WorkforceImportRow> findByBatchIdAndValidationStatusOrderByRowNumberAsc(String batchId, String validationStatus);

    void deleteByBatchId(String batchId);
}
