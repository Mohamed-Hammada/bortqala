import re

with open('be/src/main/java/com/bemo/hr/workforce/WorkforceExcelImportService.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_reverse = '''    public ImportBatchResponse reverse(String batchId) {
        WorkforceImportBatch batch = requireBatch(batchId);
        if ("REVERSED".equals(batch.getStatus())) return mapBatch(batch);
        if (!"IMPORTED".equals(batch.getStatus())) throw new BusinessRuleException("يمكن التراجع عن عملية منفذة فقط.");
        for (WorkforceImportChange change : changeRepository.findByBatchIdOrderByCreatedAtDesc(batchId)) {
            ManualAttendanceEntry entry = attendanceRepository.findById(change.getAttendanceEntryId()).orElse(null);
            if (entry == null || change.getReversedAt() != null) continue;
            if (change.isCreatedNew()) {
                entry.update(entry.getWorkerId(), entry.getWorkDate(), BigDecimal.ZERO, entry.getCheckIn(), entry.getCheckOut(),
                        entry.getActualHours(), entry.getOvertimeHours(), entry.getDeductionHours(), entry.getEffectiveDailyRate(),
                        "IMPORT_REVERSAL", "قيد عكسي لعملية الاستيراد " + batchId);
            } else {
                entry.update(entry.getWorkerId(), entry.getWorkDate(), change.getBeforeValue(), entry.getCheckIn(), entry.getCheckOut(),
                        entry.getActualHours(), entry.getOvertimeHours(), entry.getDeductionHours(), entry.getEffectiveDailyRate(),
                        change.getBeforeSource(), change.getBeforeNotes());
            }
            change.reversed();
        }
        batch.reversed(actor());
        auditService.record("REVERSE", "WORKFORCE_IMPORT", batchId, actor(),
                "{\\"changes\\":" + changeRepository.findByBatchIdOrderByCreatedAtDesc(batchId).size() + "}", null);
        return mapBatch(batchRepository.save(batch));
    }'''

new_reverse = '''    public ImportBatchResponse reverse(String batchId) {
        WorkforceImportBatch batch = requireBatch(batchId);
        if ("REVERSED".equals(batch.getStatus())) return mapBatch(batch);
        if (!"IMPORTED".equals(batch.getStatus())) throw new BusinessRuleException("يمكن التراجع عن عملية منفذة فقط.");

        List<WorkforceImportChange> changes = changeRepository.findByBatchIdOrderByCreatedAtDesc(batchId);
        Set<String> entryIdsToFetch = changes.stream()
                .filter(c -> c.getReversedAt() == null)
                .map(WorkforceImportChange::getAttendanceEntryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, ManualAttendanceEntry> entriesMap = attendanceRepository.findAllById(entryIdsToFetch).stream()
                .collect(Collectors.toMap(ManualAttendanceEntry::getId, java.util.function.Function.identity()));

        for (WorkforceImportChange change : changes) {
            ManualAttendanceEntry entry = entriesMap.get(change.getAttendanceEntryId());
            if (entry == null || change.getReversedAt() != null) continue;
            if (change.isCreatedNew()) {
                entry.update(entry.getWorkerId(), entry.getWorkDate(), BigDecimal.ZERO, entry.getCheckIn(), entry.getCheckOut(),
                        entry.getActualHours(), entry.getOvertimeHours(), entry.getDeductionHours(), entry.getEffectiveDailyRate(),
                        "IMPORT_REVERSAL", "قيد عكسي لعملية الاستيراد " + batchId);
            } else {
                entry.update(entry.getWorkerId(), entry.getWorkDate(), change.getBeforeValue(), entry.getCheckIn(), entry.getCheckOut(),
                        entry.getActualHours(), entry.getOvertimeHours(), entry.getDeductionHours(), entry.getEffectiveDailyRate(),
                        change.getBeforeSource(), change.getBeforeNotes());
            }
            change.reversed();
        }
        batch.reversed(actor());
        auditService.record("REVERSE", "WORKFORCE_IMPORT", batchId, actor(),
                "{\\"changes\\":" + changes.size() + "}", null);
        return mapBatch(batchRepository.save(batch));
    }'''

content = content.replace(old_reverse, new_reverse)

with open('be/src/main/java/com/bemo/hr/workforce/WorkforceExcelImportService.java', 'w', encoding='utf-8') as f:
    f.write(content)
