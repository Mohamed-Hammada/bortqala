package com.bemo.hr.migration.infrastructure;

import com.bemo.hr.migration.domain.DataMigrationBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataMigrationBatchRepository extends JpaRepository<DataMigrationBatch, String> {
    List<DataMigrationBatch> findAllByOrderByCreatedAtDesc();
    Optional<DataMigrationBatch> findById(String id);
}
