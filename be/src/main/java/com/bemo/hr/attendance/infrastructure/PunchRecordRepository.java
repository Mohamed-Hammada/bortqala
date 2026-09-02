package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.PunchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PunchRecordRepository extends JpaRepository<PunchRecord, String> {
    @Transactional
    void deleteByBatchId(String batchId);

    long countByBatchId(String batchId);

    long countBySourceIdAndDeviceUserIdAndPunchedAt(String sourceId, String deviceUserId, Instant punchedAt);

    Optional<PunchRecord> findBySourceIdAndDeviceUserIdAndPunchedAt(String sourceId, String deviceUserId, Instant punchedAt);

    List<PunchRecord> findBySourceIdAndPunchedAtBetweenOrderByPunchedAtAsc(String sourceId, Instant from, Instant to);

    /**
     * Removes only the punches this batch supplied that no other batch still
     * claims via punch_import_evidence. The batch's own evidence is deleted
     * before this runs, so a punch survives as long as at least one other
     * batch keeps a claim on it. Order of the two deletes no longer matters
     * because punch ownership is decided by the evidence table, not by row
     * deletion order. Scoped to the app to protect against cross-tenant
     * punch identifiers.
     */
    @Modifying
    @Query(value = """
            DELETE FROM punch_records p
            WHERE p.id IN :punchIds
              AND p.app_id = :appId
              AND NOT EXISTS (
                  SELECT 1 FROM punch_import_evidence e
                  WHERE e.punch_id = p.id
              )
            """, nativeQuery = true)
    int deleteUnclaimedPunches(@Param("appId") String appId,
                               @Param("punchIds") Collection<String> punchIds);

    @Query("select p from PunchRecord p where p.punchedAt >= :from and p.punchedAt < :to order by p.punchedAt")
    List<PunchRecord> findInRange(@Param("from") Instant from, @Param("to") Instant to);

    Optional<PunchRecord> findFirstByDeviceUserIdOrderByPunchedAtDesc(String deviceUserId);

    @Query("select year(p.punchedAt) as y, month(p.punchedAt) as m, p.deviceUserId, " +
            "count(p.id), min(p.punchedAt), max(p.punchedAt) " +
            "from PunchRecord p " +
            "where p.deviceUserId is not null and p.deviceUserId <> '' " +
            "group by year(p.punchedAt), month(p.punchedAt), p.deviceUserId")
    List<Object[]> summarizePerMonth();

    @Query("select p.deviceUserId, max(p.rawName), count(p), min(p.punchedAt), max(p.punchedAt) " +
            "from PunchRecord p where p.employeeId is null group by p.deviceUserId order by p.deviceUserId")
    List<Object[]> summarizeUnmatched();

    @Modifying
    @Query(value = """
            UPDATE punch_records
            SET employee_id = :employeeId
            WHERE app_id = :appId
              AND employee_id IS NULL
              AND device_user_id = :deviceUserId
            """, nativeQuery = true)
    int linkUnmatchedToEmployee(@Param("appId") String appId,
                                @Param("deviceUserId") String deviceUserId,
                                @Param("employeeId") String employeeId);

    /**
     * Conflict-safe insertion keyed on the punch source identity. Returns
     * {@code 1} when the row was inserted and {@code 0} when a punch with the
     * same (app, source, device user, punch time) already exists. This avoids
     * the check-then-insert race because the database decides the winner.
     */
    @Modifying
    @Query(value = """
            INSERT INTO punch_records (
                id, app_id, batch_id, source_id, device_id, employee_id,
                device_user_id, raw_name, punched_at, raw_line, row_number
            ) VALUES (
                :id, :appId, :batchId, :sourceId, :deviceId, :employeeId,
                :deviceUserId, :rawName, :punchedAt, :rawLine, :rowNumber
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("appId") String appId, @Param("batchId") String batchId,
                       @Param("sourceId") String sourceId, @Param("deviceId") String deviceId,
                       @Param("employeeId") String employeeId, @Param("deviceUserId") String deviceUserId,
                       @Param("rawName") String rawName, @Param("punchedAt") Instant punchedAt,
                       @Param("rawLine") String rawLine, @Param("rowNumber") int rowNumber);
}
