package com.bemo.hr.workforce;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkforceImportBatchRepository extends JpaRepository<WorkforceImportBatch, String> {
    Optional<WorkforceImportBatch> findByChecksum(String checksum);

    List<WorkforceImportBatch> findAllByOrderByCreatedAtDesc();

    boolean existsByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from WorkforceImportBatch b where b.id = :id")
    Optional<WorkforceImportBatch> findByIdForUpdate(@Param("id") String id);
}
