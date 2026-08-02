package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.PunchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PunchRecordRepository extends JpaRepository<PunchRecord, String> {
    boolean existsByDeviceUserIdAndPunchedAt(String deviceUserId, Instant punchedAt);

    void deleteByBatchId(String batchId);

    long countByBatchId(String batchId);

    @Query("select p from PunchRecord p where p.punchedAt >= :from and p.punchedAt < :to order by p.punchedAt")
    List<PunchRecord> findInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select p.deviceUserId, max(p.rawName), count(p), min(p.punchedAt), max(p.punchedAt) " +
           "from PunchRecord p where p.employeeId is null group by p.deviceUserId order by p.deviceUserId")
    List<Object[]> summarizeUnmatched();
}
