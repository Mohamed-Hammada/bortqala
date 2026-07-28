package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
        
        String currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
            ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
            : "system";

        auditService.record("BATCH_ATTENDANCE", "MANUAL_ATTENDANCE", 
            String.valueOf(request.entries().size()) + "_entries", 
            currentUser, "{\"date\":\"" + (request.entries().isEmpty() ? "" : request.entries().get(0).workDate()) + "\"}", null);
            
        return result;
    }

    @Transactional
    public WorkforceApi.BulkUpdateAttendanceResponse bulkUpdate(WorkforceApi.BulkUpdateAttendanceRequest request) {
        if (request.workerIds() == null || request.workerIds().isEmpty()) {
            return new WorkforceApi.BulkUpdateAttendanceResponse(0);
        }
        int count = 0;
        for (String workerId : request.workerIds()) {
            Optional<ManualAttendanceEntry> existing = attendanceRepository.findByWorkerIdAndWorkDate(workerId, request.workDate());
            if (existing.isPresent()) {
                if (request.overrideExisting()) {
                    ManualAttendanceEntry entry = existing.get();
                    entry.update(workerId, request.workDate(), request.attendanceValue(),
                            null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, "BULK", null);
                    attendanceRepository.save(entry);
                    count++;
                }
            } else {
                ManualAttendanceEntry entry = new ManualAttendanceEntry(workerId, request.workDate(), request.attendanceValue(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, "BULK", null);
                attendanceRepository.save(entry);
                count++;
            }
        }

        String currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
            ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
            : "system";
        auditService.record("BULK_ATTENDANCE_UPDATE", "MANUAL_ATTENDANCE",
                count + "_workers",
                currentUser,
                "{\"date\":\"" + request.workDate() + "\",\"value\":" + request.attendanceValue() + ",\"override\":" + request.overrideExisting() + "}",
                null);

        return new WorkforceApi.BulkUpdateAttendanceResponse(count);
    }

    @Transactional(readOnly = true)
    public WorkforceApi.CalculationRulesResponse getCalculationRules(String date) {
        BigDecimal standardHours = new BigDecimal("8");
        return new WorkforceApi.CalculationRulesResponse(
                new BigDecimal("1.5"),
                standardHours,
                BigDecimal.ZERO,
                new BigDecimal("1.5"),
                standardHours.toPlainString(),
                "احتساب الأوفرتايم: الأجر الأساسي × 1.5 للساعات الإضافية بعد " + standardHours.toPlainString()
                        + " ساعات. الخصم: حسب الأجر اليومي مقسومًا على ساعات العمل القياسية."
                        + " أيام العطل: أجر يوم كامل + 50% إضافية."
        );
    }
}
