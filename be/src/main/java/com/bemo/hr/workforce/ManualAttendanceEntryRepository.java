package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManualAttendanceEntryRepository extends JpaRepository<ManualAttendanceEntry, String> {
    Optional<ManualAttendanceEntry> findByWorkerIdAndWorkDate(String workerId, String workDate);

    List<ManualAttendanceEntry> findByWorkDate(String workDate);

    List<ManualAttendanceEntry> findByWorkerIdAndWorkDateBetween(String workerId, String startDate, String endDate);

    List<ManualAttendanceEntry> findByWorkDateBetween(String startDate, String endDate);

    List<ManualAttendanceEntry> findByWorkDateAndWorkerIdIn(String workDate, List<String> workerIds);
}
