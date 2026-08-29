package com.bemo.hr.migration.infrastructure;

import com.bemo.hr.migration.domain.DataMigrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataMigrationRecordRepository extends JpaRepository<DataMigrationRecord, String> {
    List<DataMigrationRecord> findByBatchIdOrderByRowNumberAsc(String batchId);
    long countByBatchIdAndStatus(String batchId, String status);
}
