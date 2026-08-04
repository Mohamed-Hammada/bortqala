package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.PunchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface PunchRecordRepository extends JpaRepository<PunchRecord, String> {
    @Transactional
    void deleteByBatchId(String batchId);

    long countByBatchId(String batchId);

    long countBySourceKeyAndDeviceUserIdAndPunchedAt(String sourceKey, String deviceUserId, Instant punchedAt);

    @Query("select p from PunchRecord p where p.punchedAt >= :from and p.punchedAt < :to order by p.punchedAt")
    List<PunchRecord> findInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select p.deviceUserId, max(p.rawName), count(p), min(p.punchedAt), max(p.punchedAt) " +
           "from PunchRecord p where p.employeeId is null group by p.deviceUserId order by p.deviceUserId")
    List<Object[]> summarizeUnmatched();

    /**
     * Conflict-safe insertion keyed on the punch source identity. Returns
     * {@code 1} when the row was inserted and {@code 0} when a punch with the
     * same (app, source, device user, punch time) already exists. This avoids
     * the check-then-insert race because the database decides the winner.
     */
    @Modifying
    @Query(value = """
            INSERT INTO punch_records (
                id, app_id, batch_id, source_key, device_id, employee_id,
                device_user_id, raw_name, punched_at, raw_line, row_number
            ) VALUES (
                :id, :appId, :batchId, :sourceKey, :deviceId, :employeeId,
                :deviceUserId, :rawName, :punchedAt, :rawLine, :rowNumber
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("appId") String appId, @Param("batchId") String batchId,
                       @Param("sourceKey") String sourceKey, @Param("deviceId") String deviceId,
                       @Param("employeeId") String employeeId, @Param("deviceUserId") String deviceUserId,
                       @Param("rawName") String rawName, @Param("punchedAt") Instant punchedAt,
                       @Param("rawLine") String rawLine, @Param("rowNumber") int rowNumber);
}
