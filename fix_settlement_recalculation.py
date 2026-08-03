import re

with open('be/src/main/java/com/bemo/hr/workforce/WorkforceSettlementService.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix V-22 and P1-9: Load the full in-scope active worker population for the period
old_fetch = '''        List<ManualAttendanceEntry> entries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        Map<String, List<ManualAttendanceEntry>> workerEntries = entries.stream()
                .collect(Collectors.groupingBy(ManualAttendanceEntry::getWorkerId));
        var workerIds = entries.stream().map(ManualAttendanceEntry::getWorkerId).collect(Collectors.toSet());
        List<Worker> allWorkers = workerIds.isEmpty() ? java.util.Collections.emptyList() : workerRepository.findByIdIn(workerIds);
        var contractorIds = allWorkers.stream().map(Worker::getContractorId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        List<Contractor> contractors = contractorIds.isEmpty() ? java.util.Collections.emptyList() : contractorRepository.findAllById(contractorIds);'''

new_fetch = '''        List<ManualAttendanceEntry> entries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        Map<String, List<ManualAttendanceEntry>> workerEntries = entries.stream()
                .collect(Collectors.groupingBy(ManualAttendanceEntry::getWorkerId));
        
        // Fix P1-9: Load all ACTIVE workers (or ones with entries)
        List<Worker> allWorkers = workerRepository.findAll().stream()
                .filter(w -> "ACTIVE".equalsIgnoreCase(w.getStatus()) || workerEntries.containsKey(w.getId()))
                .toList();

        var contractorIds = allWorkers.stream().map(Worker::getContractorId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        List<Contractor> contractors = contractorIds.isEmpty() ? java.util.Collections.emptyList() : contractorRepository.findAllById(contractorIds);'''

content = content.replace(old_fetch, new_fetch)

# Also fix `needsRecalculation` fingerprint 
old_needs_recalc = '''    private boolean needsRecalculation(WorkforceSettlementPeriod period) {
        if (period.getCalculationVersion() == 0 || period.getInputFingerprint() == null) return true;
        var entries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        var workerIds = entries.stream().map(ManualAttendanceEntry::getWorkerId).collect(java.util.stream.Collectors.toSet());
        var workers = workerIds.isEmpty() ? java.util.Collections.<Worker>emptyList() : workerRepository.findByIdIn(workerIds);
        return !period.getInputFingerprint().equals(inputFingerprint(entries, workers));
    }'''

new_needs_recalc = '''    private boolean needsRecalculation(WorkforceSettlementPeriod period) {
        if (period.getCalculationVersion() == 0 || period.getInputFingerprint() == null) return true;
        var entries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        var entriesByWorker = entries.stream().collect(Collectors.groupingBy(ManualAttendanceEntry::getWorkerId));
        
        // P1-9: Same worker loading logic for the fingerprint check
        List<Worker> workers = workerRepository.findAll().stream()
                .filter(w -> "ACTIVE".equalsIgnoreCase(w.getStatus()) || entriesByWorker.containsKey(w.getId()))
                .toList();

        return !period.getInputFingerprint().equals(inputFingerprint(entries, workers));
    }'''

content = content.replace(old_needs_recalc, new_needs_recalc)

with open('be/src/main/java/com/bemo/hr/workforce/WorkforceSettlementService.java', 'w', encoding='utf-8') as f:
    f.write(content)

