package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.bemo.hr.audit.application.AuditService;

@Service
@RequiredArgsConstructor
public class WorkforceAttendanceService {
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ManualAttendanceEntry> getByDateRange(String startDate, String endDate) {
        return attendanceRepository.findByWorkDateBetween(startDate, endDate);
    }

    @Transactional
    public List<ManualAttendanceEntry> saveBatch(WorkforceApi.BatchAttendanceRequest request) {
        if (request.entries() == null) return List.of();
        List<ManualAttendanceEntry> result = request.entries().stream().map(cell -> {
            Optional<ManualAttendanceEntry> existing = attendanceRepository.findByWorkerIdAndWorkDate(cell.workerId(), cell.workDate());
            BigDecimal rate = cell.effectiveDailyRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
                rate = workerRepository.findById(cell.workerId())
                    .map(Worker::getDefaultDailyRate).orElse(BigDecimal.ZERO);
            }
            if (existing.isPresent()) {
                ManualAttendanceEntry entry = existing.get();
                entry.update(
                    cell.workerId(), cell.workDate(), cell.attendanceValue(),
                    cell.checkIn(), cell.checkOut(), cell.actualHours(),
                    cell.overtimeHours(), cell.deductionHours(), rate, "MANUAL", cell.notes()
                );
                return attendanceRepository.save(entry);
            } else {
                ManualAttendanceEntry entry = new ManualAttendanceEntry(
                    cell.workerId(), cell.workDate(), cell.attendanceValue(),
                    cell.checkIn(), cell.checkOut(), cell.actualHours(),
                    cell.overtimeHours(), cell.deductionHours(), rate, "MANUAL", cell.notes()
                );
                return attendanceRepository.save(entry);
            }
        }).toList();
        
        auditService.record("BATCH_ATTENDANCE", "MANUAL_ATTENDANCE", 
            String.valueOf(request.entries().size()) + "_entries", 
            "system", "{\"date\":\"" + (request.entries().isEmpty() ? "" : request.entries().get(0).workDate()) + "\"}", null);
            
        return result;
    }
}
