package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    public WorkforceApi.BatchAttendanceResponse saveBatch(WorkforceApi.BatchAttendanceRequest request) {
        if (request.entries() == null || request.entries().isEmpty()) {
            return new WorkforceApi.BatchAttendanceResponse(0, 0, 0, 0, List.of(), List.of());
        }

        var errors = new ArrayList<WorkforceApi.AttendanceCellError>();
        var validCells = new ArrayList<WorkforceApi.AttendanceCell>();
        var seenKeys = new HashSet<String>();
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (var cell : request.entries()) {
            String field = validateCell(cell);
            if (field != null) {
                errors.add(error(cell, field, validationMessage(field)));
                continue;
            }
            String cellKey = key(cell.workerId(), cell.workDate());
            if (!seenKeys.add(cellKey)) {
                errors.add(error(cell, "cell", "تم إرسال العامل والتاريخ نفسيهما أكثر من مرة."));
                continue;
            }
            LocalDate workDate = LocalDate.parse(cell.workDate());
            minDate = minDate == null || workDate.isBefore(minDate) ? workDate : minDate;
            maxDate = maxDate == null || workDate.isAfter(maxDate) ? workDate : maxDate;
            validCells.add(cell);
        }

        Set<String> workerIds = validCells.stream().map(WorkforceApi.AttendanceCell::workerId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, Worker> workers = new HashMap<>();
        workerRepository.findAllById(workerIds).forEach(worker -> workers.put(worker.getId(), worker));

        var processable = new ArrayList<WorkforceApi.AttendanceCell>();
        for (var cell : validCells) {
            if (!workers.containsKey(cell.workerId())) {
                errors.add(error(cell, "workerId", "العامل غير موجود أو لا يتبع الشركة الحالية."));
            } else {
                processable.add(cell);
            }
        }

        Map<String, ManualAttendanceEntry> existingByKey = new HashMap<>();
        if (minDate != null && maxDate != null) {
            attendanceRepository.findByWorkDateBetween(minDate.toString(), maxDate.toString()).forEach(entry ->
                    existingByKey.put(key(entry.getWorkerId(), entry.getWorkDate()), entry));
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        var toSave = new ArrayList<ManualAttendanceEntry>();
        for (var cell : processable) {
            Worker worker = workers.get(cell.workerId());
            BigDecimal rate = cell.effectiveDailyRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
                rate = worker.getDefaultDailyRate() == null ? BigDecimal.ZERO : worker.getDefaultDailyRate();
            }
            ManualAttendanceEntry existing = existingByKey.get(key(cell.workerId(), cell.workDate()));
            if (existing != null) {
                if (existing.hasSameManualValues(cell.attendanceValue(), cell.checkIn(), cell.checkOut(),
                        cell.actualHours(), cell.overtimeHours(), cell.deductionHours(), rate, cell.notes())) {
                    skipped++;
                    continue;
                }
                existing.update(cell.workerId(), cell.workDate(), cell.attendanceValue(), cell.checkIn(),
                        cell.checkOut(), cell.actualHours(), cell.overtimeHours(), cell.deductionHours(), rate,
                        "MANUAL", cell.notes());
                toSave.add(existing);
                updated++;
            } else {
                toSave.add(new ManualAttendanceEntry(cell.workerId(), cell.workDate(), cell.attendanceValue(),
                        cell.checkIn(), cell.checkOut(), cell.actualHours(), cell.overtimeHours(),
                        cell.deductionHours(), rate, "MANUAL", cell.notes()));
                created++;
            }
        }
        List<ManualAttendanceEntry> savedEntries = attendanceRepository.saveAll(toSave);

        String currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
            ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
            : "system";

        auditService.record("BATCH_ATTENDANCE", "MANUAL_ATTENDANCE",
            String.valueOf(request.entries().size()) + "_entries",
            currentUser, "{\"created\":" + created + ",\"updated\":" + updated
                    + ",\"skipped\":" + skipped + ",\"failed\":" + errors.size() + "}", null);

        return new WorkforceApi.BatchAttendanceResponse(created, updated, skipped, errors.size(),
                savedEntries, errors);
    }

    private String validateCell(WorkforceApi.AttendanceCell cell) {
        if (cell == null) return "cell";
        if (cell.workerId() == null || cell.workerId().isBlank()) return "workerId";
        if (cell.workDate() == null || cell.workDate().isBlank()) return "workDate";
        try { LocalDate.parse(cell.workDate()); } catch (DateTimeParseException exception) { return "workDate"; }
        if (cell.attendanceValue() == null || (cell.attendanceValue().compareTo(BigDecimal.ZERO) != 0
                && cell.attendanceValue().compareTo(new BigDecimal("0.5")) != 0
                && cell.attendanceValue().compareTo(BigDecimal.ONE) != 0)) return "attendanceValue";
        if (negative(cell.actualHours())) return "actualHours";
        if (negative(cell.overtimeHours())) return "overtimeHours";
        if (negative(cell.deductionHours())) return "deductionHours";
        if (negative(cell.effectiveDailyRate())) return "effectiveDailyRate";
        if (cell.notes() != null && cell.notes().length() > 500) return "notes";
        return null;
    }

    private boolean negative(BigDecimal value) { return value != null && value.compareTo(BigDecimal.ZERO) < 0; }
    private String key(String workerId, String workDate) { return workerId + "|" + workDate; }
    private WorkforceApi.AttendanceCellError error(WorkforceApi.AttendanceCell cell, String field, String message) {
        return new WorkforceApi.AttendanceCellError(cell == null ? null : cell.workerId(),
                cell == null ? null : cell.workDate(), field, message);
    }
    private String validationMessage(String field) {
        return switch (field) {
            case "workDate" -> "التاريخ غير صالح؛ استخدم الصيغة YYYY-MM-DD.";
            case "attendanceValue" -> "قيمة الحضور يجب أن تكون 0 أو 0.5 أو 1.";
            case "actualHours", "overtimeHours", "deductionHours" -> "عدد الساعات لا يمكن أن يكون سالباً.";
            case "effectiveDailyRate" -> "الأجر اليومي لا يمكن أن يكون سالباً.";
            case "notes" -> "الملاحظات لا يمكن أن تتجاوز 500 حرف.";
            case "workerId" -> "العامل مطلوب.";
            default -> "بيانات الخلية غير صالحة.";
        };
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
