package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.ImportBatch;
import com.bemo.hr.attendance.domain.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, String> {
    Optional<ImportBatch> findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc(
            String sourceId, String checksum, ImportStatus status);

    Optional<ImportBatch> findBySourceIdAndChecksum(String sourceId, String checksum);

    List<ImportBatch> findAllByOrderByImportedAtDesc();

    /**
     * Atomically reserves a batch keyed on (app, source, checksum). Returns
     * {@code 1} when this call created the batch and {@code 0} when another
     * concurrent import already reserved the same content, so duplicate file
     * uploads and identical device syncs never create two batches. The batch
     * row is pre-populated with the parse-level metrics; punch outcomes are
     * finalized on the loaded entity after the row loop.
     */
    @Modifying
    @Query(value = """
            INSERT INTO import_batches (
                id, app_id, checksum, file_name, source_id, device_name, status,
                total_rows, imported_rows, valid_rows, new_punches, duplicate_punches,
                error_rows, imported_by, imported_at
            ) VALUES (
                :id, :appId, :checksum, :fileName, :sourceId, :deviceName, :status,
                :totalRows, :validRows, :validRows, 0, 0, :errorRows, :importedBy, CURRENT_TIMESTAMP
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("appId") String appId,
                       @Param("checksum") String checksum, @Param("fileName") String fileName,
                       @Param("sourceId") String sourceId, @Param("deviceName") String deviceName,
                       @Param("status") String status, @Param("totalRows") int totalRows,
                       @Param("validRows") int validRows, @Param("errorRows") int errorRows,
                       @Param("importedBy") String importedBy);
}
